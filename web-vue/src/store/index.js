import Vuex from "vuex";
import Vue from "vue";

import user from "./modules/user";
import app from "./modules/app";
import guide from "./modules/guide";
import locale from "./modules/locale";
import menu from "./modules/menu";
import managementMenu from "./modules/management-menu";

Vue.use(Vuex);

const store = new Vuex.Store({
  modules: {
    user,
    app,
    guide,
    locale,
    menu,
    managementMenu,
  },
});

export default store;
