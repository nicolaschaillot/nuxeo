/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void setGitHubBuildStatus(String context, String message, String state) {
  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/nuxeo/nuxeo'],
    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
    statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
  ])
}

String getVersion() {
  String nuxeoVersion = readMavenPom().getVersion()
  return BRANCH_NAME == 'master' ? nuxeoVersion : nuxeoVersion + "-${BRANCH_NAME}-${BUILD_NUMBER}"
}

pipeline {
  agent {
    label 'jenkins-nuxeo-java'
  }
  environment {
    HELM_CHART_REPOSITORY_NAME = 'local-jenkins-x'
    HELM_CHART_REPOSITORY_URL = 'http://jenkins-x-chartmuseum:8080'
    HELM_CHART_NUXEO_REDIS = 'nuxeo-redis'
    HELM_RELEASE_REDIS = 'redis'
    SERVICE_REDIS = 'redis-master'
    SERVICE_ACCOUNT = 'jenkins'
    ORG = 'nuxeo'
  }
  stages {
    stage('Compile') {
      steps {
        setGitHubBuildStatus('platform/compile', 'Compile', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh 'mvn -B -T0.8C -Pdistrib -DskipTests install'
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/compile', 'Compile', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/compile', 'Compile', 'FAILURE')
        }
      }
    }
    stage('Run "dev" unit tests') {
      steps {
        setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Install Redis
          ----------------------------------------"""
          // initialize Helm without installing Tiller
          sh "helm init --client-only --service-account ${SERVICE_ACCOUNT}"

          // add local chart repository
          sh "helm repo add ${HELM_CHART_REPOSITORY_NAME} ${HELM_CHART_REPOSITORY_URL}"

          // install the nuxeo-redis chart
          // use 'jx step helm install' to avoid 'Error: incompatible versions' when running 'helm install'
          sh """
            jx step helm install ${HELM_CHART_REPOSITORY_NAME}/${HELM_CHART_NUXEO_REDIS} --name ${HELM_RELEASE_REDIS}
          """

          echo """
          ----------------------------------------
          Run "dev" unit tests
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh "mvn -B -Dnuxeo.test.redis.host=${SERVICE_REDIS} test"
          }
        }
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'FAILURE')
        }
      }
    }
    stage('Build and deploy Docker image') {
      steps {
        setGitHubBuildStatus('platform/docker', 'Build and deploy Docker image', 'PENDING')
        container('maven') {
          withEnv(["VERSION=${getVersion()}"]) {
            echo """
            ----------------------------------------
            Build and deploy Docker image
            ----------------------------------------
            Image tag: ${VERSION}
            """
            sh 'skaffold build -f nuxeo-distribution/nuxeo-server-tomcat/skaffold.yaml'
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/docker', 'Build and deploy Docker image', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/docker', 'Build and deploy Docker image', 'FAILURE')
        }
      }
    }
  }
  post {
    always {
      script {
        if (BRANCH_NAME == 'master') {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
  }
}
