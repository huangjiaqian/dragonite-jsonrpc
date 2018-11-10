# dragonite-jsonrpc
基于jsonrpc2协议，使用dragonite-java实现的用于远程调用的jsonrpc工具包

### 详情
- JSON-RPC 2.0详情：https://www.jsonrpc.org/specification
- dragonite-java详情：https://github.com/dragonite-network/dragonite-java

### 介绍
 本项目是采用JSON-RPC 2.0协议，通过rudp框架dragonite-java构建的用于远程调用的工具包

### 使用方法
  - 首先使用lib文件夹的dragonite-jsonrpc-0.0.1-SNAPSHOT.jar包或者自己构建jar包。
#### 客户端
  例子：
  ```java
  public class ClientTest {
    public static void main(String[] args) throws SocketException {
      InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 12222);
      DragoniteClientSocket dragoniteSocket = new DragoniteClientSocket(remoteAddress, 1024 * 100, new DragoniteSocketParameters());
      JsonRpcClientServiceFactory factory = new JsonRpcClientServiceFactory(dragoniteSocket);
      IUserDao userDao = factory.getService(IUserDao.class);
      System.out.println(userDao.getUserName());
    }
  }
  ```
#### 服务器端
  例子：
  ```java
  public class ServerTest {
    public static void main(String[] args) throws SocketException {
      DragoniteServer dragoniteServer = new DragoniteServer(12222, 1024 * 100, new DragoniteSocketParameters());
      JsonRpcServer jsonRpcServer = new JsonRpcServer(dragoniteServer, new ServerServiceConfig() {

        @Override
        public void config(List<Object> cfgList) {

          cfgList.add(new UserDao());

        }
      });
      jsonRpcServer.start();
    }
  }
  ```
