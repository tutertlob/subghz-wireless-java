# im920-java
A java package for communicating among IM920 wireless modules from Interplan.

通信モジュールIM920を使用するためのjavaライブラリ。Arduino用ライブラリは[こちら](https://github.com/tutertlob/im920-arduino)。

IM920でデータ通信するため本ライブラリでは４つのパケットタイプを定義し、IM920のフレームに乗せて送信する。パケットタイプの詳細については[Arduino用ライブラリ](https://github.com/tutertlob/im920-arduino)を参照。

## Installation
im920-javaのソースディレクトで
```
mvn install
```

最後にrxtxのnativeライブラリをインストールする。詳細はDependenciesを参照。

## Configuring IM920 wireless module
IM920通信モジュールを設定する。Interplanから出ているUSB interface boardを使用し、PCと接続して行う。
以下の項目を設定する。設定コマンドはInterplanのマニュアルを参照。送受信側双方同じ設定にする。
* ボーレート: 38,400 bps（初期値: 19,200 bps）
* 動作モード: データモード（初期値: データモード）
* キャラクタ入出力: DCIO（初期値: DCIO)
* 受信ID登録(初期値: 未登録)
  </br>送信側（相手）モジュールのIDを自身に登録する。双方向通信する場合はお互いのモジュールIDを登録し合う。

## Quick usage
```
try {
    im920Interface = Im920Interface.open("/dev/ttyUSB0", Im920Interface.BaudRate.B_38400);
    im920 = new Im920(im920Interface);
} catch (IOException | NoSuchPortException | PortInUseException e) {
    // exception handling here
}

im920.sendNotice("Hello world!!");
 ```

## Dependencies
### RXTX native libraries
[こちら](http://rxtx.qbang.org/wiki/index.php/Installation_on_Linux)に沿ってインストールする。パッケージマネージャを使用してインストールする方法とソースファイルからライブラリをビルドして手動でインストールする方法がある。

#### Installing rxtx native lib with package manager
パッケージマネージャを使ってrxtxのJNIライブラリをインストールする。
````
sudo apt-get install librxtx-java
````
ライブラリは`/usr/lib/jni`にインストールされる。インストールバージョンは2.2pre2。

`RXTX`を使用する際は、`java.library.path`にインストールディレクトリを指定する。
```
sudo java -Djava.library.path=/usr/lib/jni -jar ...
```
ただ[こちら](http://rxtx.qbang.org/wiki/index.php/Download)に'TODO: The 2.2pre2 bins contain the 2.2pre1 jar file and the 2.2pre2 native lib which causes a mismatch warning'
とあるようにmavenレポジトリのjarファイル（バージョン2.2pre1)とミスマッチが発生し、下記の通りWARNINGが発生する。
```
WARNING:  RXTX Version mismatch
	Jar version = RXTX-2.2pre1
	native lib Version = RXTX-2.2pre2
```
もしWARNINGを解消したい場合はrxtxのソースファイルをビルドしjarファイルを2.2pre2にバージョンアップするか、jniライブラリのバージョンを2.2pre1に下げる必要がある。解消しなくても問題ない。


#### Installing rxtx native lib from source files
rxtxのWARNINGを解消したい場合など、ソースをビルドしてインストールするには以下のようにする。

ビルドに必要なツールをインストールする。
```
sudo apt-get install build-essential raspberrypi-kernel-headers autoconf automake libtool libtool-bin
```
[こちら](http://rxtx.qbang.org/wiki/index.php/Download)からrxtxのソースファイル(2.2pre1)をダウンロードする。
ソースディレクトリにて
```
./configure
make
```
makeでUTS_RELEASEが未定義としてエラーが発生し、ビルドが途中で終了する。ただ、今回必要なシリアルのライブラリ`librxtxSerial-2.2pre1.so`は生成されているので問題ない。共有ライブラリをコピーしシンボリックリンクを作成する。
```
$ ls armv7l-unknown-linux-gnu/.libs/
ParallelImp.o  librxtxParallel-2.2pre1.so  librxtxParallel.so        librxtxSerial.lai
SerialImp.o    librxtxParallel.la          librxtxSerial-2.2pre1.so  librxtxSerial.so
fuserImp.o     librxtxParallel.lai         librxtxSerial.la

$ sudo cp armv7l-unknown-linux-gnu/.libs/librxtxSerial-2.2pre1.so /usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/arm
$ cd /usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/arm
$ sudo ln -s librxtxSerial-2.2pre1.so librxtxSerial.so
```
`/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/arm`にjniライブラリをコピーした場合、`java.library.path`でjniのパスを指定する必要はない。

## Latest rxtx jar file
Mavenのレポジトリや開発元サイトで提供されているrxtx jarバイナリは実際のバージョンが2.2pre2ではなく2.2pre1となっており、rxtxのNativeライブラリとのバージョンミスマッチが発生する。ミスマッチが発生しても動作するがWarningメッセージを解消したい場合は、次の方法で最新版のjarファイルをビルドしMavenローカルレポジトリにインストールする。
#### Installing latest rxtx jar file from source files
rxtxのjarファイルを2.2pre2にバージョンアップする。最新版のソースファイルをダウンロードし、ビルドする。同様のビルドエラーが発生するが、rxtxのjarファイル`RXTXcomm.jar`は作成されているので問題ない。

次にrxtxのjarファイルをmavenローカルレポジトリにインストールする。
```
mvn install:install-file -Dfile=RXTXcomm.jar -DgroupId=org.rxtx -DartifactId=rxtx -Dversion=2.2pre2 -Dpackaging=jar -DgeneratePom=true
```

