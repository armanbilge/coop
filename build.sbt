/*
 * Copyright 2020 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

ThisBuild / tlBaseVersion := "1.2"

ThisBuild / developers := List(tlGitHubDev("djspiewak", "Daniel Spiewak"))

ThisBuild / crossScalaVersions := Seq("3.1.2", "2.12.15", "2.13.8")

ThisBuild / organization := "com.armanbilge"
ThisBuild / tlSonatypeUseLegacyHost := false
ThisBuild / tlCiReleaseBranches += "publish/native"
ThisBuild / tlMimaPreviousVersions := Set.empty
ThisBuild / resolvers += "s01" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"
ThisBuild / tlCiScalafmtCheck := false
ThisBuild / tlCiHeaderCheck := false

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform).in(file("core"))
  .settings(
    name := "coop",
    libraryDependencies += "org.specs2" %%% "specs2-core" % "4.16.0" % Test)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-free" % "2.8.0",
      "com.armanbilge" %%% "cats-mtl"  % "1.3-a6f62d6-SNAPSHOT"))
