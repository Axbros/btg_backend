/**
 * BTG Commission Backend — Spring Boot 3 + Maven + Java 17
 *
 * Jenkins 准备：
 * 1. 「全局工具配置」中配置 JDK（建议名称：JDK-17）与 Maven（建议名称：Maven-3.9），
 *    并将下方 tools 里的名称改成与你环境一致。
 * 2. 若不用 tools，可删掉 tools 块，并在 agent 镜像或节点 PATH 中提供 java 17 + mvn。
 */
pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '5'))
    }

    parameters {
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: '勾选则 mvn 加 -DskipTests')
    }

    tools {
        jdk 'JDK-17'
        maven 'Maven-3.9'
    }

    environment {
        // 与 pom.xml artifactId、version 一致时可固定；否则用通配符归档
        JAR_GLOB = 'target/btg-commission-backend-*.jar'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    def skipTests = params.SKIP_TESTS ? '-DskipTests' : ''
                    sh "mvn -B -V clean verify ${skipTests} -Dmaven.test.failure.ignore=false"
                }
            }
        }

        stage('Archive') {
            when {
                expression { return fileExists('target') }
            }
            steps {
                archiveArtifacts artifacts: "${JAR_GLOB}", fingerprint: true, onlyIfSuccessful: true
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/**/*.xml', allowEmptyResults: true
        }
        success {
            echo "构建成功。可执行 jar：${JAR_GLOB}"
        }
        failure {
            echo '构建失败，请查看日志与 Surefire 报告。'
        }
    }
}
