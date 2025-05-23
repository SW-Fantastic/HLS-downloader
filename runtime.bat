

SET JAVA_HOME=D:\Program Files (x86)\Java SDK\jdk-21
SET PATH=%JAVA_HOME%\bin;%PATH%

:: 注意，这个是使用jdk16的jpackage打包的脚本，
:: 所以运行前请在此环境准备jdk16以及jdk16打包需要的WIX
:: wix需要放入PATH环境变量中。
:: 运行此脚本打包前需要运行maven的package。

echo cleaning...

rmdir /s /q deploy
mkdir deploy

rem 定义应用名称
set name="HLSDownloader"
rem 定义模块名称
set moduleName="org.swdc.hls"
rem 定义启动类全限定名
set mainClass="org.swdc.hls.Launcher"
rem 定义javafx的jmod所在位置
set javafxJmodPath="D:\SDK\javafx-jmods-21.0.1"
rem 应用的图标
set iconName="favicon.ico"

echo start jlink for runtime...
rem 进行连接，生成JRE环境
jlink --output deploy\runtime --module-path %javafxJmodPath% --add-modules java.instrument,java.base,java.naming,java.scripting,javafx.fxml,javafx.controls,javafx.base,javafx.graphics,jdk.jfr,java.datatransfer,java.prefs,java.xml,java.sql,java.transaction.xa,java.desktop,jdk.unsupported,java.instrument,javafx.web,javafx.media,java.compiler,java.xml.crypto,jdk.unsupported.desktop,java.net.http,jdk.jsobject,jdk.xml.dom,java.prefs,java.transaction.xa,java.logging,javafx.swing,java.management.rmi,jdk.zipfs,jdk.crypto.ec,jdk.crypto.cryptoki


echo deploy application....

rem 执行打包，注意，打包之后的lib里面会附带很多javafx的包，这些应该删掉，他们都已经在jre里面了，多了的话无法启动
cd deploy
copy ..\target\*.jar ..\target\lib
jpackage --runtime-image runtime --type app-image -n %name% -p ../target/lib --icon ../icon.ico -m %moduleName%/%mainClass%
del /a /f /q %name%\app\mods\javafx-*.jar
copy ..\libs\*.jar %name%\app\mods
echo coping resources...
xcopy /E/I/Y ..\assets\ %name%\assets\
echo done.

cd ../