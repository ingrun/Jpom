export default {
  c: {
    buildMethod: '构建方式',
    name: '名称',
    selectRepository: '请选择仓库',
    chooseRepository: '选择仓库',
    supportWildcard: '支持通配符',
    matchOneCharacter: '匹配一个字符',
    matchZeroOrMoreCharacters: '匹配零个或多个字符',
    matchZeroOrMoreDirectories: '匹配路径中的零个或多个目录',
    configuration: '配置',
    configurationExample: '配置示例',
    publishOperation: '发布操作',
    secondLevelDirectory: '二级目录',
    sshNotConfigured: '如果 ssh 没有配置授权目录是不能选择的哟',
    selectSSH: '请选择SSH',
    sshDirectoryConfiguration: '如果多选 ssh 下面目录只显示选项中的第一项，但是授权目录需要保证每项都配置对应目录',
    yes: '是',
    no: '否',
    selectScript: '请选择脚本',
    chooseScript: '选择脚本',
    cancel: '取消',
    confirm: '确认'
  },
  p: {
    loadingBuildData: '加载构建数据中',
    howToChooseBuildMethod: '如何选择构建方式',
    viewAvailableContainers: '查看当前可用容器',
    availableTags: '可用标签',
    loadingContainerTags: '加载容器可用标签中....',
    noContainerOrTag: '还没有容器或者未配置标签不可以使用容器构建奥',
    containerBuildNote: '容器构建注意',
    groupName: '分组名称：',
    addGroup: '新增分组',
    selectGroup: '选择分组',
    sourceRepository: '源仓库',
    branch: '分支',
    customBranchWildcard: '自定义分支通配表达式',
    selectBranchForBuild: '请选择构建对应的分支,必选',
    tag: '标签',
    customTagWildcard: '自定义标签通配表达式',
    selectTagForBuild: '选择构建的标签,不选为最新提交',
    cloneDepth: '克隆深度',
    customCloneDepth: '自定义克隆深度，避免大仓库全部克隆',
    buildCommand: '构建命令',
    commonBuildCommandExample: '常见构建命令示例',
    scriptTemplate: '引用脚本模板',
    content: '内容',
    buildDslConfigContent: '请填写构建 DSL 配置内容,可以点击上方切换 tab 查看配置示例',
    artifactDirectory: '产物目录',
    onlyUseFirstMatched: '【目前只使用匹配到的第一项】',
    noBuildMethodSelected: '还没有选择构建方式',
    environmentVariables: '环境变量',
    enterBuildEnvVars: '请输入构建环境变量：xx=abc 多个变量回车换行即可',
    executionMethod: '执行方式',
    default: '默认',
    multiThread: '多线程',
    experimentalFeature: '此选项为一个实验属性实际效果基本无差异',
    selectPublishMethod: '请选择发布方式',
    noPublish: '不发布：只执行构建流程并且保存构建历史',
    noPublishProcess: '不执行发布流程',
    distributeProject: '分发项目',
    selectDistributeProject: '请选择分发项目',
    useNodeDistributeConfig: '不填写则使用节点分发配置的二级目录',
    publishProject: '发布项目',
    selectNodeProject: '请选择节点项目',
    publishPostOperation: '请选择发布后操作',
    publishToRoot: '不填写则发布至项目的根目录',
    publishViaSSH: '发布的SSH',
    publishDirectory: '发布目录',
    prePublishCommand: '发布前命令',
    nonMandatory: '非必填',
    postPublishCommand: '发布后命令',
    mandatory: '必填',
    clearPublish: '清空发布',
    diffPublish: '差异发布：',
    preStopPublish: '发布前停止：',
    executeContainer: '执行容器',
    executeContainerTag: '执行容器 标签',
    dockerfilePath:
      '需要在仓库里面 dockerfile,如果多文件夹查看可以指定二级目录如果 springboot-test-jar:springboot-test-jar/Dockerfile',
    imageTag: '镜像 tag',
    containerTag:
      '容器标签,如：xxxx:latest 多个使用逗号隔开, 配置附加环境变量文件支持加载仓库目录下 .env 文件环境变量 如： xxxx:${VERSION}',
    buildParams: '构建参数',
    buildParamsInput: '构建参数,如：key1=values1&keyvalue2 使用 URL 编码',
    imageTagInput: '镜像标签,如：key1=values1&keyvalue2 使用 URL 编码',
    publishCluster: '发布集群',
    dockerSwarmCluster: '目前使用的 docker swarm 集群，需要先创建 swarm 集群才能选择',
    selectClusterForPublish: '请选择发布到哪个 docker 集群',
    noClusterPublish: '不发布到 docker 集群',
    pushToRepository: '镜像构建成功后是否需要推送到远程仓库',
    versionIncrement: '版本递增',
    noCacheBuild: '构建镜像的过程不使用缓存',
    updateImage: '更新镜像',
    attemptToUpdateBaseImage: '构建镜像尝试去更新基础镜像的新版本',
    clusterService: '集群服务',
    selectServiceForPublish: '需要选发布到集群中的对应的服务名，需要提前去集群中创建服务',
    cacheBuild: '缓存构建',
    retainBuildArtifacts: '保留产物：',
    diffBuild: '差异构建：',
    strictExecution: '严格执行：',
    buildProcessRequest: '构建过程请求,非必填，GET请求',
    eventScript: '事件脚本',
    resetSelection: '重置选择',
    additionalEnvVars: '附加环境变量',
    additionalEnvVarsInput: '附加环境变量  .env 新增多个使用逗号分隔',
    fileManagementCenter: '文件管理中心',
    syncToFile: '如果开启同步到文件管理中心，在构建发布流程将自动执行同步到文件管理中心的操作。',
    sync: '同步',
    noSync: '不同步',
    publishHiddenFiles: '发布隐藏文件',
    defaultIgnoreHiddenFiles: '默认构建错误将自动忽略隐藏文件',
    publishAllFiles: '开启此选项后可以正常发布隐藏文件',
    retainDays: '保留天数',
    retainCount: '保留个数',
    aliasCode: '别名码',
    aliasCodeInput: '如果产物同步到文件中心,当前值会共享',
    enterAliasCode: '请输入别名码',
    generateAliasCode: '随机生成',
    retainDaysForArtifacts: '构建产物同步到文件中心保留天数',
    excludePublish: '排除发布',
    excludePublishAntExpression: '使用 ANT 表达式来实现在过滤指定目录来实现发布排除指定目录',
    viewContainer: '查看容器',
    buildCommandExample: '构建命令示例',
    buildName: '请填写构建名称',
    buildMethod: '请选择构建方式',
    publishAction: '请选择发布操作',
    selectBranch: '请选择分支',
    writeBuildCommand: '请填写构建命令',
    writeArtifactDirectory: '请填写产物目录',
    repositorySelection: '请填选择构建的仓库',
    basicInfo: '基础信息',
    buildProcess: '构建流程',
    additionalConfig: '其他配置',
    unknown: '未知',
    nodeProjectSelection: '请选择节点项目,可能是节点中不存在任何项目,需要去节点中创建项目',
    buildArtifactsPath: '构建产物目录,相对仓库的路径,如 java 项目的 target/xxx.jar vue 项目的 dist',
    postBuildActions: '发布后操作',
    uploadToDirectory: '构建产物上传到对应目录',
    dockerfilePathTip: '文件夹路径 需要在仓库里面 dockerfile',
    containerTags: '容器标签,如：xxxx:latest 多个使用逗号隔开',
    buildParamsTip: '构建参数,如：key1=value1&key2=value2',
    imageTags: '镜像标签：',
    imageTagParamsTip: '镜像标签,如：key1=value1&key2=value2',
    pushToRepositoryLabel: '推送到仓库',
    selectedServiceForPublish: '请选择发布到集群的服务名',
    cacheBuildDirectory:
      '开启缓存构建目录将保留仓库文件,二次构建将 pull 代码, 不开启缓存目录每次构建都将重新拉取仓库代码(较大的项目不建议关闭缓存)',
    retainBuildArtifactsInfo: '保留产物是指对在构建完成后是否保留构建产物相关文件，用于回滚',
    incrementalBuild: '差异构建是指构建时候是否判断仓库代码有变动，如果没有变动则不执行构建',
    timedBuild: '定时构建',
    cronExpression:
      '如果需要定时自动构建则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）',
    retentionDays: '保留天数：',
    exclusionForPublish: '排除发布 ANT 表达式,多个使用逗号分隔',
    //
    howToList1: '本地构建是指直接在服务端中的服务器执行构建命令',
    howToList2: '注意执行相关命令需要所在服务器中存在对应的环境',
    howToList3: '并且配置正确的环境变量',
    howToList4: '如果是在启动服务端后安装并配置的环境变量需要通过终端命令来重启服务端才能生效',
    howToList5: '容器构建是指使用 docker 容器执行构建,这样可以达到和宿主机环境隔离不用安装依赖环境',
    howToList6: '使用容器构建，docker 容器所在的宿主机需要有公网,因为需要远程下载环境依赖的 sdk 和镜像',
    howToList7: '创建后构建方式不支持修改',
    howToList8:
      '容器安装的服务端不能使用本地构建（因为本地构建依赖启动服务端本地的环境，容器方式安装不便于管理本地依赖插件）',
    containerList1: '实现您需要配置 docker 容器到服务端中来管理，并且分配到当前工作空间中',
    containerList2: '为当前工作空间中的容器配置标签',
    containerList3: '需要将标签值配置到构建 DSL 中的',
    containerList4: '字段',
    buildCommandL1: '这里构建命令最终会在服务器上执行。如果有多行命令那么将',
    buildCommandL2: '逐行执行',
    buildCommandL3: '，如果想要切换路径后执行命令则需要',
    buildCommandHelp:
      '构建执行的命令(非阻塞命令)，如：mvn clean package、npm run build。支持变量：${ BUILD_ID }、${ BUILD_NAME }、${ BUILD_SOURCE_FILE }、${ BUILD_NUMBER_ID }、仓库目录下 .env、工作空间变量',
    dsl1: '以 yaml/yml 格式配置',
    dsl2: '配置需要声明使用具体的 docker 来执行构建相关操作(建议使用服务端所在服务器中的 docker)',
    dsl3: '容器构建会在 docker 中生成相关挂载目录,一般情况不需要人为操作',
    dsl4: '执行构建时会生成一个容器来执行，构建结束后会自动删除对应的容器',
    dsl5: '目前支持都插件有（更多插件尽情期待）：',
    dsl6: 'java sdk 镜像使用：https://mirrors.tuna.tsinghua.edu.cn/ 支持版本有：8, 9, 10, 11, 12, 13, 14, 15, 16, 17',
    dsl7: 'maven sdk 镜像使用：https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/',
    dsl8: 'node sdk 镜像使用：https://registry.npmmirror.com/-/binary/node',
    dsl9: '(存在兼容问题,实际使用中需要提前测试) python3 sdk 镜像使用：https://repo.huaweicloud.com/python/${PYTHON3_VERSION}/Python-${PYTHON3_VERSION}.tar.xz',
    dsl10:
      '(存在兼容问题,实际使用中需要提前测试) go sdk 镜像使用：https://studygolang.com/dl/golang/go${GO_VERSION}.linux-${ARCH}.tar.gz',
    artifact1: '可以理解为项目打包的目录。 如 Jpom 项目执行（构建命令）',
    artifact2: '构建命令，构建产物相对路径为：',
    publish1: '发布操作是指,执行完构建命令后将构建产物目录中的文件用不同的方式发布(上传)到对应的地方',
    publish2: '节点分发是指,一个项目部署在多个节点中使用节点分发一步完成多个节点中的项目发布操作',
    publish3: '项目是指,节点中的某一个项目,需要提前在节点中创建项目',
    publish4: 'SSH 是指,通过 SSH 命令的方式对产物进行发布或者执行多条命令来实现发布(需要到 SSH 中提前去新增)',
    publish5: '本地命令是指,在服务端本地执行多条命令来实现发布',
    publish6:
      ' SSH、本地命令发布都执行变量替换,系统预留变量有：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}',
    publish7: '可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称',
    releasePath2P: '发布目录,构建产物上传到对应目录',
    prePublish1: '发布前执行的命令(非阻塞命令),一般是关闭项目命令',
    prePublish2: '支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}',
    prePublish3: '可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称',
    prePublishHelp:
      '发布前执行的命令(非阻塞命令),一般是关闭项目命令,支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}',
    postPublish1: '发布后执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java',
    postPublish2: '支持变量替换：${BUILD_ID}、${BUILD_NAME}、${BUILD_RESULT_FILE}、${BUILD_NUMBER_ID}',
    postPublish3: '可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称',
    postPublishHelp:
      '发布后执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java, 支持变量替换：${ BUILD_ID }、${ BUILD_NAME }、${ BUILD_RESULT_FILE }、${ BUILD_NUMBER_ID } ',
    clearPublishTip: '清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件',
    diffTip1: '差异发布是指对应构建产物和项目文件夹里面的文件是否存在差异,如果存在增量差异那么上传或者覆盖文件。',
    diffTip2:
      '开启差异发布并且开启清空发布时将自动删除项目目录下面有的文件但是构建产物目录下面没有的文件【清空发布差异上传前会先执行删除差异文件再执行上传差异文件】',
    diffTip3: '开启差异发布但不开启清空发布时相当于只做增量和变动更新',
    preStopPublishTip:
      '发布前停止是指在发布文件到项目文件时先将项目关闭，再进行文件替换。避免 windows 环境下出现文件被占用的情况',
    executeContainerTip:
      '使用哪个 docker 构建,填写 docker 标签（ 标签在 docker 编辑页面配置） 默认查询可用的第一个,如果tag 查询出多个将依次构建',
    versionIncrementTip:
      '开启 dockerTag 版本递增后将在每次构建时自动将版本号最后一位数字同步为构建序号ID, 如：当前构建为第 100 次构建 testtag:1.0 -> testtag:1.100,testtag:1.0.release -> testtag:1.100.release。如果没有匹配到数字将忽略递增操作',
    cacheBuildTip:
      '开启缓存构建目录将保留仓库文件,二次构建将 pull 代码, 不开启缓存目录每次构建都将重新拉取仓库代码(较大的项目不建议关闭缓存) 、特别说明如果缓存目录中缺失版本控制相关文件将自动删除后重新拉取代码',
    strictExecutionTip:
      '严格执行脚本（构建命令、事件脚本、本地发布脚本、容器构建命令）执行返回状态码必须是 0、否则将构建状态标记为失败',
    webHookTip1: '构建过程请求对应的地址,开始构建,构建完成,开始发布,发布完成,构建异常,发布异常',
    webHookTip2: '传入参数有：buildId、buildName、type、statusMsg、triggerTime',
    webHookTip3: 'type 的值有：startReady、pull、executeCommand、release、done、stop、success、error',
    webHookTip4: '异步请求不能保证有序性',
    eventScriptTip1: '构建过程执行对应的脚本,开始构建,构建完成,开始发布,发布完成,构建异常,发布异常',
    eventScriptTip2: '传入环境变量有：buildId、buildName、type、statusMsg、triggerTime、buildNumberId、buildSourceFile',
    eventScriptTip3: '执行脚本传入参数有：startReady、pull、executeCommand、release、done、stop、success',
    eventScriptTip4:
      '注意：为了避免不必要的事件执行脚本，选择的脚本的备注中包含需要实现的事件参数关键词，如果需要执行 success 事件,那么选择的脚本的备注中需要包含 success 关键词'
  }
}
