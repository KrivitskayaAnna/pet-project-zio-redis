addSbtPlugin("com.eed3si9n" %% "sbt-assembly" % "1.1.0")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")

libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.2"
