version = "1.0.0"

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":plugins:civmodcore-paper"))
    compileOnly(project(":plugins:citadel-paper"))
    compileOnly(project(":plugins:civchat2-paper"))
    compileOnly(project(":plugins:factorymod-paper"))
    compileOnly(project(":plugins:realisticbiomes-paper"))
    compileOnly(project(":plugins:hiddenore-paper"))
    compileOnly(project(":plugins:itemexchange-paper"))
    compileOnly(project(":plugins:exilepearl-paper"))
    compileOnly(project(":plugins:jukealert-paper"))
    compileOnly(project(":plugins:namelayer-paper"))
    compileOnly(project(":plugins:bastion-paper"))
    compileOnly(libs.bundles.evenmorefish)
    compileOnly(files("../../ansible/src/paper-plugins/BreweryX-3.6.0.jar"))
}
