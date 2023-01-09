/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileCopier;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.model.user.UserModel;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.script.ScriptExecuteLogServer;
import io.jpom.service.script.ScriptServer;
import io.jpom.service.system.WorkspaceEnvVarService;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.extconf.BuildExtConfig;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import io.jpom.util.LogRecorder;
import io.jpom.util.StringUtil;
import lombok.Builder;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Service
@Slf4j
public class BuildExecuteService {

    /**
     * 缓存构建中
     */
    private static final Map<String, BuildInfoManage> BUILD_MANAGE_MAP = new ConcurrentHashMap<>();

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /**
     * 构建线程池
     */
    private static ThreadPoolExecutor threadPoolExecutor;

    private final BuildInfoService buildService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final RepositoryService repositoryService;
    protected final DockerInfoService dockerInfoService;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final ScriptServer scriptServer;
    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final BuildExtConfig buildExtConfig;

    public BuildExecuteService(BuildInfoService buildService,
                               DbBuildHistoryLogService dbBuildHistoryLogService,
                               RepositoryService repositoryService,
                               DockerInfoService dockerInfoService,
                               WorkspaceEnvVarService workspaceEnvVarService,
                               ScriptServer scriptServer,
                               ScriptExecuteLogServer scriptExecuteLogServer,
                               BuildExtConfig buildExtConfig) {
        this.buildService = buildService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.repositoryService = repositoryService;
        this.dockerInfoService = dockerInfoService;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.scriptServer = scriptServer;
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.buildExtConfig = buildExtConfig;
    }

    /**
     * 创建构建线程池
     */
    private synchronized void initPool() {
        if (threadPoolExecutor != null) {
            return;
        }
        ExecutorBuilder executorBuilder = ExecutorBuilder.create();
        int poolSize = buildExtConfig.getPoolSize();
        if (poolSize > 0) {
            executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
        }
        executorBuilder.useArrayBlockingQueue(Math.max(buildExtConfig.getPoolWaitQueue(), 1));
        executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                if (r instanceof BuildInfoManage) {
                    // 取消任务
                    BuildInfoManage buildInfoManage = (BuildInfoManage) r;
                    buildInfoManage.rejectedExecution();
                }
            }
        });
        threadPoolExecutor = executorBuilder.build();
    }

    /**
     * check status
     *
     * @param status 状态码
     * @return 错误消息
     */
    public String checkStatus(Integer status) {
        if (status == null) {
            return null;
        }
        BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, status);
        Objects.requireNonNull(nowStatus);
        if (BuildStatus.Ing == nowStatus ||
            BuildStatus.PubIng == nowStatus) {
            return "当前还在：" + nowStatus.getDesc();
        }
        return null;
    }

    /**
     * start build
     *
     * @param buildInfoId      构建Id
     * @param userModel        用户信息
     * @param delay            延迟的时间
     * @param triggerBuildType 触发构建类型
     * @param buildRemark      构建备注
     * @return json
     */
    public JsonMessage<Integer> start(String buildInfoId, UserModel userModel, Integer delay, int triggerBuildType, String buildRemark) {
        return this.start(buildInfoId, userModel, delay, triggerBuildType, buildRemark, null);
    }

    /**
     * start build
     *
     * @param buildInfoId         构建Id
     * @param userModel           用户信息
     * @param delay               延迟的时间
     * @param triggerBuildType    触发构建类型
     * @param buildRemark         构建备注
     * @param checkRepositoryDiff 差异构建
     * @return json
     */
    public JsonMessage<Integer> start(String buildInfoId, UserModel userModel, Integer delay, int triggerBuildType, String buildRemark, String checkRepositoryDiff) {
        synchronized (buildInfoId.intern()) {
            BuildInfoModel buildInfoModel = buildService.getByKey(buildInfoId);
            String e = this.checkStatus(buildInfoModel.getStatus());
            Assert.isNull(e, () -> e);
            // set buildId field
            int buildId = ObjectUtil.defaultIfNull(buildInfoModel.getBuildId(), 0);
            {
                BuildInfoModel buildInfoModel1 = new BuildInfoModel();
                buildInfoModel1.setBuildId(buildId + 1);
                buildInfoModel1.setId(buildInfoId);
                buildInfoModel.setBuildId(buildInfoModel1.getBuildId());
                buildService.update(buildInfoModel1);
            }
            // load repository
            RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId(), false);
            Assert.notNull(repositoryModel, "仓库信息不存在");
            Map<String, String> env = workspaceEnvVarService.getEnv(buildInfoModel.getWorkspaceId());
            BuildExecuteService.TaskData.TaskDataBuilder taskBuilder = BuildExecuteService.TaskData.builder()
                .buildInfoModel(buildInfoModel)
                .repositoryModel(repositoryModel)
                .userModel(userModel)
                .buildRemark(buildRemark)
                .delay(delay).env(env)
                .triggerBuildType(triggerBuildType);
            //
            Opt.ofBlankAble(checkRepositoryDiff).map(Convert::toBool).ifPresent(taskBuilder::checkRepositoryDiff);
            this.runTask(taskBuilder.build());
            String msg = (delay == null || delay <= 0) ? "开始构建中" : "延迟" + delay + "秒后开始构建";
            return new JsonMessage<>(200, msg, buildInfoModel.getBuildId());
        }
    }

    /**
     * 创建构建
     *
     * @param taskData 任务
     */
    private void runTask(TaskData taskData) {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        boolean containsKey = BUILD_MANAGE_MAP.containsKey(buildInfoModel.getId());
        Assert.state(!containsKey, "当前构建还在进行中");
        //
        BuildExtraModule buildExtraModule = StringUtil.jsonConvert(buildInfoModel.getExtraData(), BuildExtraModule.class);
        Assert.notNull(buildExtraModule, "构建信息缺失");
        String logId = this.insertLog(buildExtraModule, taskData);
        // 创建线程池
        initPool();
        //
        BuildInfoManage.BuildInfoManageBuilder builder = BuildInfoManage.builder()
            .taskData(taskData)
            .logId(logId)
            .buildExtraModule(buildExtraModule)
            .buildExecuteService(this);
        BuildInfoManage build = builder.build();
        //BuildInfoManage manage = new BuildInfoManage(taskData);
        BUILD_MANAGE_MAP.put(buildInfoModel.getId(), build);
        // 输出提交任务日志, 提交到线程池中
        threadPoolExecutor.execute(build.submitTask());
    }

    /**
     * 取消构建
     *
     * @param id id
     * @return bool
     */
    public boolean cancelTask(String id) {
        return Optional.ofNullable(BUILD_MANAGE_MAP.get(id)).map(buildInfoManage1 -> {
            buildInfoManage1.cancelTask();
            return true;
        }).orElse(false);
    }


    /**
     * 插入记录
     */
    private String insertLog(BuildExtraModule buildExtraModule, TaskData taskData) {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        buildExtraModule.updateValue(buildInfoModel);
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        // 更新其他配置字段
        //buildHistoryLog.fillLogValue(buildExtraModule);
        buildHistoryLog.setTriggerBuildType(taskData.triggerBuildType);
        //
        buildHistoryLog.setBuildNumberId(buildInfoModel.getBuildId());
        buildHistoryLog.setBuildName(buildInfoModel.getName());
        buildHistoryLog.setBuildDataId(buildInfoModel.getId());
        buildHistoryLog.setWorkspaceId(buildInfoModel.getWorkspaceId());
        buildHistoryLog.setResultDirFile(buildInfoModel.getResultDirFile());
        buildHistoryLog.setReleaseMethod(buildExtraModule.getReleaseMethod());
        //
        buildHistoryLog.setStatus(BuildStatus.Ing.getCode());
        buildHistoryLog.setStartTime(SystemClock.now());
        buildHistoryLog.setBuildRemark(taskData.buildRemark);
        buildHistoryLog.setExtraData(buildInfoModel.getExtraData());
        dbBuildHistoryLogService.insert(buildHistoryLog);
        //
        buildService.updateStatus(buildHistoryLog.getBuildDataId(), BuildStatus.Ing);
        return buildHistoryLog.getId();
    }

    /**
     * 更新状态
     *
     * @param buildId     构建ID
     * @param logId       记录ID
     * @param buildStatus to status
     */
    public void updateStatus(String buildId, String logId, int buildNumberId, BuildStatus buildStatus) {
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setId(logId);
        buildHistoryLog.setStatus(buildStatus.getCode());
        if (buildStatus != BuildStatus.PubIng) {
            // 结束
            buildHistoryLog.setEndTime(SystemClock.now());
        }
        dbBuildHistoryLogService.update(buildHistoryLog);
        buildService.updateStatus(buildId, buildNumberId, buildStatus);
    }

    /**
     * 更新最后一次构建完成的仓库代码 last commit
     *
     * @param buildId      构建ID
     * @param lastCommitId 最后一次的仓库代码 last commit
     */
    private void updateLastCommitId(String buildId, String lastCommitId) {
        BuildInfoModel buildInfoModel = new BuildInfoModel();
        buildInfoModel.setId(buildId);
        buildInfoModel.setRepositoryLastCommitId(lastCommitId);
        buildService.update(buildInfoModel);
    }

    @Builder
    public static class TaskData {
        private final BuildInfoModel buildInfoModel;
        private final RepositoryModel repositoryModel;
        private final UserModel userModel;
        /**
         * 延迟执行的时间（单位秒）
         */
        private final Integer delay;
        /**
         * 触发类型
         */
        private final int triggerBuildType;
        /**
         * 构建备注
         */
        private String buildRemark;
        /**
         * 环境变量
         */
        private Map<String, String> env;

        /**
         * 仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）
         */
        private String repositoryLastCommitId;
        /**
         * 是否差异构建
         */
        private Boolean checkRepositoryDiff;
    }


    @Builder
    private static class BuildInfoManage implements Runnable {

        private final TaskData taskData;
        private final BuildExtraModule buildExtraModule;
        private final String logId;
        private final BuildExecuteService buildExecuteService;
        //
        private Process process;
        private LogRecorder logRecorder;
        private File gitFile;
        private Thread currentThread;
        /**
         * 提交任务时间
         */
        private Long submitTaskTime;

        private final Map<String, String> buildEnv = new HashMap<>(10);

        /**
         * 提交任务
         */
        public BuildInfoManage submitTask() {
            submitTaskTime = SystemClock.now();
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;
            File logFile = BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
            this.logRecorder = LogRecorder.builder().file(logFile).build();
            //
            int queueSize = threadPoolExecutor.getQueue().size();
            int size = BUILD_MANAGE_MAP.size();
            logRecorder.info("当前构建中任务数：{},队列中任务数：{} {}", size, queueSize,
                size > buildExecuteService.buildExtConfig.getPoolSize() ? "构建任务开始进入队列等待...." : StrUtil.EMPTY);
            return this;
        }

        /**
         * 取消任务(拒绝执行)
         */
        public void rejectedExecution() {
            int queueSize = threadPoolExecutor.getQueue().size();
            logRecorder.info("当前构建中任务数：{},队列中任务数：{} 构建任务等待超时或者超出最大等待数量,取消执行当前构建", BUILD_MANAGE_MAP.size(), queueSize);
            this.cancelTask();
        }

        /**
         * 取消任务
         */
        public void cancelTask() {
            Optional.ofNullable(process).ifPresent(Process::destroy);
            Optional.ofNullable(currentThread).ifPresent(Thread::interrupt);

            String buildId = taskData.buildInfoModel.getId();
            buildExecuteService.updateStatus(buildId, logId, taskData.buildInfoModel.getBuildId(), BuildStatus.Cancel);
            BUILD_MANAGE_MAP.remove(buildId);
        }

        /**
         * 打包构建产物
         */
        private boolean packageFile() {
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;
            Integer buildMode = taskData.buildInfoModel.getBuildMode();
            String resultDirFile = buildInfoModel.getResultDirFile();
            if (buildMode != null && buildMode == 1) {
                // 容器构建直接下载到 结果目录
                File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFile);
                if (!FileUtil.exist(toFile)) {
                    logRecorder.info(resultDirFile + "不存在，处理构建产物失败");
                    return false;
                }
                logRecorder.info(StrUtil.format("mv {} {}", resultDirFile, buildInfoModel.getBuildId()));
                return true;
            }
            ThreadUtil.sleep(1, TimeUnit.SECONDS);
            boolean updateDirFile = false;
            boolean copyFile = true;
            if (ANT_PATH_MATCHER.isPattern(resultDirFile)) {
                // 通配模式
                List<String> paths = FileUtils.antPathMatcher(this.gitFile, resultDirFile);
                int size = CollUtil.size(paths);
                if (size <= 0) {
                    logRecorder.info(resultDirFile + " 没有匹配到任何文件");
                    return false;
                }
                if (size == 1) {
                    String first = CollUtil.getFirst(paths);
                    // 切换到匹配到到文件
                    logRecorder.info(StrUtil.format("match {} {}", resultDirFile, first));
                    resultDirFile = first;
                    updateDirFile = true;
                } else {
                    resultDirFile = FileUtil.normalize(resultDirFile);
                    logRecorder.info(StrUtil.format("match {} count {}", resultDirFile, size));
                    String subBefore = StrUtil.subBefore(resultDirFile, "*", false);
                    subBefore = StrUtil.subBefore(subBefore, StrUtil.SLASH, true);
                    subBefore = StrUtil.emptyToDefault(subBefore, StrUtil.SLASH);
                    resultDirFile = subBefore;
                    copyFile = false;
                    updateDirFile = true;
                    for (String path : paths) {
                        File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), subBefore);
                        FileCopier.create(FileUtil.file(this.gitFile, path), FileUtil.file(toFile, path))
                            .setCopyContentIfDir(true).setOverride(true).setCopyAttributes(true)
                            .setCopyFilter(file1 -> !file1.isHidden())
                            .copy();
                    }
                }
            }
            if (copyFile) {
                File file = FileUtil.file(this.gitFile, resultDirFile);
                if (!file.exists()) {
                    logRecorder.info(resultDirFile + "不存在，处理构建产物失败");
                    return false;
                }
                File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFile);
                FileCopier.create(file, toFile)
                    .setCopyContentIfDir(true).setOverride(true).setCopyAttributes(true)
                    .setCopyFilter(file1 -> !file1.isHidden())
                    .copy();
            }
            logRecorder.info(StrUtil.format("mv {} {}", resultDirFile, buildInfoModel.getBuildId()));
            // 修改构建产物目录
            if (updateDirFile) {
                buildExecuteService.dbBuildHistoryLogService.updateResultDirFile(this.logId, resultDirFile);
                //
                buildInfoModel.setResultDirFile(resultDirFile);
                this.buildExtraModule.setResultDirFile(resultDirFile);
            }
            return true;
        }

        /**
         * 准备构建
         *
         * @return false 执行异常需要结束
         */
        private boolean startReady() {
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;
            this.gitFile = BuildUtil.getSourceById(buildInfoModel.getId());

            Integer delay = taskData.delay;
            logRecorder.info("#" + buildInfoModel.getBuildId() + " start build in file : " + FileUtil.getAbsolutePath(this.gitFile));
            if (delay != null && delay > 0) {
                // 延迟执行
                logRecorder.info("Execution delayed by " + delay + " seconds");
                ThreadUtil.sleep(delay, TimeUnit.SECONDS);
            }
            // 删除缓存
            Boolean cacheBuild = this.buildExtraModule.getCacheBuild();
            if (cacheBuild != null && !cacheBuild) {
                logRecorder.info("clear cache");
                CommandUtil.systemFastDel(this.gitFile);
            }
            //
            buildEnv.put("BUILD_ID", this.buildExtraModule.getId());
            buildEnv.put("BUILD_NAME", this.buildExtraModule.getName());
            buildEnv.put("BUILD_SOURCE_FILE", FileUtil.getAbsolutePath(this.gitFile));
            buildEnv.put("BUILD_NUMBER_ID", this.taskData.buildInfoModel.getBuildId() + "");
            return true;
        }

        /**
         * 拉取代码
         *
         * @return false 执行异常需要结束
         */
        private boolean pull() {
            RepositoryModel repositoryModel = taskData.repositoryModel;
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;
            try {
                String msg = "error";
                Integer repoTypeCode = repositoryModel.getRepoType();
                RepositoryModel.RepoType repoType = EnumUtil.likeValueOf(RepositoryModel.RepoType.class, repoTypeCode);
                Boolean checkRepositoryDiff = Optional.ofNullable(taskData.checkRepositoryDiff).orElse(buildExtraModule.getCheckRepositoryDiff());
                String repositoryLastCommitId = buildInfoModel.getRepositoryLastCommitId();
                if (repoType == RepositoryModel.RepoType.Git) {
                    // git with password
                    IPlugin plugin = PluginFactory.getPlugin("git-clone");
                    Map<String, Object> map = repositoryModel.toMap();
                    Tuple tuple = (Tuple) plugin.execute("branchAndTagList", map);
                    //GitUtil.getBranchAndTagList(repositoryModel);
                    Assert.notNull(tuple, "获取仓库分支失败");
                    map.put("reduceProgressRatio", this.buildExecuteService.buildExtConfig.getLogReduceProgressRatio());
                    map.put("logWriter", logRecorder.getPrintWriter());
                    map.put("savePath", gitFile);
                    // 模糊匹配 标签
                    String branchTagName = buildInfoModel.getBranchTagName();
                    String[] result;
                    if (StrUtil.isNotEmpty(branchTagName)) {
                        String newBranchTagName = BuildExecuteService.fuzzyMatch(tuple.get(1), branchTagName);
                        if (StrUtil.isEmpty(newBranchTagName)) {
                            logRecorder.info(branchTagName + " Did not match the corresponding tag");
                            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), BuildStatus.Error);
                            return false;
                        }
                        // author bwcx_jzy 2022.11.28 map.put("branchName", newBranchName);
                        map.put("tagName", newBranchTagName);
                        //author bwcx_jzy 2022.11.28 buildEnv.put("BUILD_BRANCH_NAME", newBranchName);
                        buildEnv.put("BUILD_TAG_NAME", newBranchTagName);
                        // 标签拉取模式
                        logRecorder.info("repository tag [{}] clone pull from {}", branchTagName, newBranchTagName);
                        result = (String[]) plugin.execute("pullByTag", map);
                    } else {
                        String branchName = buildInfoModel.getBranchName();
                        // 模糊匹配分支
                        String newBranchName = BuildExecuteService.fuzzyMatch(tuple.get(0), branchName);
                        if (StrUtil.isEmpty(newBranchName)) {
                            logRecorder.info(branchName + " Did not match the corresponding branch");
                            buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), BuildStatus.Error);
                            return false;
                        }
                        // 分支模式
                        map.put("branchName", newBranchName);
                        buildEnv.put("BUILD_BRANCH_NAME", newBranchName);
                        logRecorder.info("repository [" + branchName + "] clone pull from " + newBranchName);
                        result = (String[]) plugin.execute("pull", map);
                    }
                    msg = result[1];
                    // 判断hash 码和上次构建是否一致
                    if (checkRepositoryDiff != null && checkRepositoryDiff) {
                        if (StrUtil.equals(repositoryLastCommitId, result[0])) {
                            // 如果一致，则不构建
                            logRecorder.info("仓库代码没有任何变动终止本次构建：{} {}", result[0], msg);
                            return false;
                        }
                    }
                    taskData.repositoryLastCommitId = result[0];
                } else if (repoType == RepositoryModel.RepoType.Svn) {
                    // svn
                    Map<String, Object> map = repositoryModel.toMap();

                    IPlugin plugin = PluginFactory.getPlugin("svn-clone");
                    String[] result = (String[]) plugin.execute(gitFile, map);
                    //msg = SvnKitUtil.checkOut(repositoryModel, gitFile);
                    msg = ArrayUtil.get(result, 1);
                    // 判断版本号和上次构建是否一致
                    if (checkRepositoryDiff != null && checkRepositoryDiff) {
                        if (StrUtil.equals(repositoryLastCommitId, result[0])) {
                            // 如果一致，则不构建
                            logRecorder.info("仓库代码没有任何变动终止本次构建：{}", result[0]);
                            return false;
                        }
                    }
                    taskData.repositoryLastCommitId = result[0];
                }
                logRecorder.info(msg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        private boolean dockerCommand() {
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;
            String script = buildInfoModel.getScript();
            DockerYmlDsl dockerYmlDsl = DockerYmlDsl.build(script);
            String fromTag = dockerYmlDsl.getFromTag();
            // 根据 tag 查询
            List<DockerInfoModel> dockerInfoModels = buildExecuteService
                .dockerInfoService
                .queryByTag(buildInfoModel.getWorkspaceId(), 1, fromTag);
            DockerInfoModel dockerInfoModel = CollUtil.getFirst(dockerInfoModels);
            Assert.notNull(dockerInfoModel, "没有可用的 docker server");
            logRecorder.info("use docker {}", dockerInfoModel.getName());
            String workingDir = "/home/jpom/";
            Map<String, Object> map = dockerInfoModel.toParameter();
            map.put("runsOn", dockerYmlDsl.getRunsOn());
            map.put("workingDir", workingDir);
            map.put("tempDir", JpomApplication.getInstance().getTempPath());
            String buildInfoModelId = buildInfoModel.getId();
            map.put("dockerName", "jpom-build-" + buildInfoModelId);
            map.put("logFile", FileUtil.getAbsolutePath(logRecorder.getFile()));
            //
            List<String> copy = ObjectUtil.defaultIfNull(dockerYmlDsl.getCopy(), new ArrayList<>());
            // 将仓库文件上传到容器
            copy.add(FileUtil.getAbsolutePath(this.gitFile) + StrUtil.COLON + workingDir + StrUtil.COLON + "true");
            map.put("copy", copy);
            map.put("binds", ObjectUtil.defaultIfNull(dockerYmlDsl.getBinds(), new ArrayList<>()));

            Map<String, String> dockerEnv = ObjectUtil.defaultIfNull(dockerYmlDsl.getEnv(), new HashMap<>(10));
            Map<String, String> env = taskData.env;
            env.putAll(dockerEnv);
            env.put("JPOM_BUILD_ID", buildInfoModelId);
            env.put("JPOM_WORKING_DIR", workingDir);
            map.put("env", env);
            map.put("steps", dockerYmlDsl.getSteps());
            // 构建产物
            String resultDirFile = buildInfoModel.getResultDirFile();
            String resultFile = FileUtil.normalize(workingDir + StrUtil.SLASH + resultDirFile);
            map.put("resultFile", resultFile);
            // 产物输出目录
            File toFile = BuildUtil.getHistoryPackageFile(buildInfoModelId, buildInfoModel.getBuildId(), resultDirFile);
            map.put("resultFileOut", FileUtil.getAbsolutePath(toFile));
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
            try {
                plugin.execute("build", map);
            } catch (Exception e) {
                logRecorder.error("构建调用容器异常", e);
                return false;
            }
            return true;
        }

        /**
         * 执行构建命令
         *
         * @return false 执行异常需要结束
         */
        private boolean executeCommand() {
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;
            Integer buildMode = buildInfoModel.getBuildMode();
            if (buildMode != null && buildMode == 1) {
                // 容器构建
                return this.dockerCommand();
            }
            if (StrUtil.isEmpty(buildInfoModel.getScript())) {
                logRecorder.info("没有需要执行的命令");
                this.buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), BuildStatus.Error);
                return false;
            }
            Map<String, String> environment = new HashMap<>(10);
            environment.putAll(taskData.env);
            // env file
            Map<String, String> envFileMap = FileUtils.readEnvFile(this.gitFile, this.buildExtraModule.getAttachEnv());
            environment.putAll(envFileMap);
            environment.putAll(buildEnv);

            InputStream templateInputStream = ExtConfigBean.getConfigResourceInputStream("/exec/template." + CommandUtil.SUFFIX);
            String s1 = IoUtil.readUtf8(templateInputStream);
            try {
                int waitFor = JpomApplication.getInstance()
                    .execScript(s1 + buildInfoModel.getScript(), file -> {
                        try {
                            return CommandUtil.execWaitFor(file, this.gitFile, environment, StrUtil.EMPTY, (s, process) -> logRecorder.info(s));
                        } catch (IOException | InterruptedException e) {
                            throw Lombok.sneakyThrow(e);
                        }
                    });
                logRecorder.info("[SYSTEM-INFO] --- PROCESS RESULT " + waitFor);
            } catch (Exception e) {
                logRecorder.error("执行异常", e);
                return false;
            }
            return true;
        }

        /**
         * 打包发布
         *
         * @return false 执行需要结束
         */
        private boolean release() {
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;
            UserModel userModel = taskData.userModel;
            // 发布文件
            ReleaseManage releaseManage = ReleaseManage.builder()
                .buildNumberId(buildInfoModel.getBuildId())
                .buildExtraModule(buildExtraModule)
                .userModel(userModel)
                .logId(logId)
                .buildEnv(buildEnv)
                .buildExecuteService(buildExecuteService)
                .logRecorder(logRecorder).build();
            try {
                return releaseManage.start();
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        }

        /**
         * 结束流程
         *
         * @return 流程执行是否成功
         */
        private boolean finish() {
            BuildInfoModel buildInfoModel1 = taskData.buildInfoModel;
            buildExecuteService.updateLastCommitId(buildInfoModel1.getId(), taskData.repositoryLastCommitId);
            //
            BuildStatus buildStatus = buildInfoModel1.getReleaseMethod() != BuildReleaseMethod.No.getCode() ? BuildStatus.PubSuccess : BuildStatus.Success;
            buildExecuteService.updateStatus(buildInfoModel1.getId(), this.logId, this.taskData.buildInfoModel.getBuildId(), buildStatus);
            return true;
        }

        private Map<String, IProcessItem> createProcess() {
            // 初始化构建流程 准备构建->拉取仓库代码->执行构建命令->打包产物->发布产物->构建结束
            Map<String, IProcessItem> suppliers = new LinkedHashMap<>(10);
            suppliers.put("startReady", new IProcessItem() {
                @Override
                public String name() {
                    return "准备构建";
                }

                @Override
                public boolean execute() {
                    return BuildInfoManage.this.startReady();
                }
            });
            suppliers.put("pull", new IProcessItem() {
                @Override
                public String name() {
                    return "拉取仓库代码";
                }

                @Override
                public boolean execute() {
                    return BuildInfoManage.this.pull();
                }
            });
            suppliers.put("executeCommand", new IProcessItem() {
                @Override
                public String name() {
                    return "执行构建命令";
                }

                @Override
                public boolean execute() {
                    return BuildInfoManage.this.executeCommand();
                }
            });
            suppliers.put("packageFile", new IProcessItem() {
                @Override
                public String name() {
                    return "打包产物";
                }

                @Override
                public boolean execute() {
                    return BuildInfoManage.this.packageFile();
                }
            });
            suppliers.put("release", new IProcessItem() {
                @Override
                public String name() {
                    return "发布产物";
                }

                @Override
                public boolean execute() {
                    return BuildInfoManage.this.release();
                }
            });
            suppliers.put("finish", new IProcessItem() {
                @Override
                public String name() {
                    return "构建结束";
                }

                @Override
                public boolean execute() {
                    return BuildInfoManage.this.finish();
                }
            });
            return suppliers;
        }

        @Override
        public void run() {
            currentThread = Thread.currentThread();
            logRecorder.info("开始执行构建任务,任务等待时间：{}", DateUtil.formatBetween(SystemClock.now() - submitTaskTime));
            Map<String, IProcessItem> processItemMap = this.createProcess();
            // 依次执行流程，发生异常结束整个流程
            String processName = StrUtil.EMPTY;
            long startTime = SystemClock.now();
            if (taskData.triggerBuildType == 2) {
                // 系统触发构建
                BaseServerController.resetInfo(UserModel.EMPTY);
            } else {
                BaseServerController.resetInfo(taskData.userModel);
            }
            BuildInfoModel buildInfoModel = this.taskData.buildInfoModel;
            try {
                for (Map.Entry<String, IProcessItem> stringSupplierEntry : processItemMap.entrySet()) {
                    processName = stringSupplierEntry.getKey();
                    IProcessItem processItem = stringSupplierEntry.getValue();
                    //
                    long processItemStartTime = SystemClock.now();
                    logRecorder.info("[SYSTEM-INFO] 开始执行 {}流程", processItem.name());
                    this.asyncWebHooks(processName);
                    boolean aBoolean = processItem.execute();
                    if (!aBoolean) {
                        // 有条件结束构建流程
                        logRecorder.info("[SYSTEM-INFO] 执行异常 {}流程", processItem.name());
                        this.asyncWebHooks("stop", "process", processName);
                        buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error);
                        break;
                    }
                    logRecorder.info("[SYSTEM-INFO] 执行结束 {}流程,耗时：{}", processItem.name(), DateUtil.formatBetween(SystemClock.now() - processItemStartTime));
                }
                // 判断是否保留产物
                Boolean saveBuildFile = this.buildExtraModule.getSaveBuildFile();
                if (saveBuildFile != null && !saveBuildFile) {
                    //
                    File historyPackageFile = BuildUtil.getHistoryPackageFile(buildExtraModule.getId(), buildInfoModel.getBuildId(), StrUtil.SLASH);
                    CommandUtil.systemFastDel(historyPackageFile);
                }
                this.asyncWebHooks("success");
            } catch (RuntimeException runtimeException) {
                buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error);
                Throwable cause = runtimeException.getCause();
                logRecorder.error("构建失败:" + processName, cause == null ? runtimeException : cause);
                this.asyncWebHooks(processName, "error", runtimeException.getMessage());
            } catch (Exception e) {
                buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, buildInfoModel.getBuildId(), BuildStatus.Error);
                logRecorder.error("构建失败:" + processName, e);
                this.asyncWebHooks(processName, "error", e.getMessage());
            } finally {
                logRecorder.info("[SYSTEM-INFO] 构建结束 累计耗时:{}", DateUtil.formatBetween(SystemClock.now() - startTime));
                this.asyncWebHooks("done");
                BUILD_MANAGE_MAP.remove(buildInfoModel.getId());
                BaseServerController.removeAll();
            }
//            return false;
        }

//		private void log(String title, Throwable throwable) {
//			log(title, throwable, BuildStatus.Error);
//		}

        /**
         * 执行 webhooks 通知
         *
         * @param type  类型
         * @param other 其他参数
         */
        private void asyncWebHooks(String type, Object... other) {
            BuildInfoModel buildInfoModel = taskData.buildInfoModel;

            IPlugin plugin = PluginFactory.getPlugin("webhook");
            Map<String, Object> map = new HashMap<>(10);
            long triggerTime = SystemClock.now();
            map.put("buildId", buildInfoModel.getId());
            map.put("buildNumberId", this.taskData.buildInfoModel.getBuildId());
            map.put("buildName", buildInfoModel.getName());
            map.put("buildSourceFile", FileUtil.getAbsolutePath(this.gitFile));
            map.put("type", type);
            map.put("triggerBuildType", taskData.triggerBuildType);
            map.put("triggerTime", triggerTime);
            map.put("buildResultFile", BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), this.taskData.buildInfoModel.getBuildId(), buildExtraModule.getResultDirFile()));
            //
            for (int i = 0; i < other.length; i += 2) {
                map.put(other[i].toString(), other[i + 1]);
            }
            String webhook = buildInfoModel.getWebhook();
            Opt.ofBlankAble(webhook).ifPresent(s -> ThreadUtil.execute(() -> {
                try {
                    plugin.execute(s, map);
                } catch (Exception e) {
                    log.error("WebHooks 调用错误", e);
                }
            }));
            // 执行对应的事件脚本
            try {
                this.noticeScript(type, map);
            } catch (Exception e) {
                log.error("noticeScript 调用错误", e);
            }
        }

        /**
         * 执行事件脚本
         *
         * @param type 事件类型
         * @param map  相关参数
         * @throws Exception 异常
         */
        private void noticeScript(String type, Map<String, Object> map) throws Exception {
            String noticeScriptId = this.buildExtraModule.getNoticeScriptId();
            if (StrUtil.isEmpty(noticeScriptId)) {
                return;
            }
            ScriptModel scriptModel = buildExecuteService.scriptServer.getByKey(noticeScriptId);
            if (scriptModel == null) {
                logRecorder.info("[SYSTEM-WARNING] noticeScript does not exist:{}", type);
                return;
            }
            // 判断是否包含需要执行的事件
            if (!StrUtil.contains(scriptModel.getDescription(), type)) {
                log.warn("忽略执行事件脚本 {} {} {}", type, scriptModel.getName(), noticeScriptId);
                return;
            }
            logRecorder.info("[SYSTEM-INFO] --- EXEC NOTICESCRIPT {}", type);
            // 环境变量
            Map<String, String> environment = new HashMap<>(map.size());
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                environment.put(entry.getKey(), StrUtil.toStringOrNull(value));
            }
            ScriptExecuteLogModel logModel = buildExecuteService.scriptExecuteLogServer.create(scriptModel, 1);
            File logFile = scriptModel.logFile(logModel.getId());
            File scriptFile = null;
            try (LogRecorder scriptLog = LogRecorder.builder().file(logFile).build()) {
                // 创建执行器
                scriptFile = scriptModel.scriptFile();
                int waitFor = JpomApplication.getInstance().execScript(scriptModel.getContext(), file -> {
                    try {
                        return CommandUtil.execWaitFor(file, null, environment, null, (s, process) -> {
                            logRecorder.info(s);
                            scriptLog.info(s);
                        });
                    } catch (IOException | InterruptedException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
                logRecorder.info("[SYSTEM-INFO] {} --- NOTICESCRIPT PROCESS RESULT {}", type, waitFor);
            } finally {
                if (scriptFile != null) {
                    try {
                        FileUtil.del(scriptFile);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * 模糊匹配
     *
     * @param list    待匹配待列表
     * @param pattern 迷糊的表达式
     * @return 匹配到到值
     */
    private static String fuzzyMatch(List<String> list, String pattern) {
        Assert.notEmpty(list, "仓库没有任何分支或者标签");
        if (ANT_PATH_MATCHER.isPattern(pattern)) {
            List<String> collect = list.stream().filter(s -> ANT_PATH_MATCHER.match(pattern, s)).collect(Collectors.toList());
            return CollUtil.getFirst(collect);
        }
        return pattern;
    }
}
