// AUTHOR: Marin Govedarski
// NOTE: This is a highly-specific "pipeline" that may be replicdnted across different environments and scenarios. This would be mots appropriate for Jenkins-based CI (& CD) approaches, especially if you have existant jobs that are used commonly. 

// This script aims to automate a ready set of existing deployment jobs that require manual effort and thus create a "click-ops" approach, inducing a lot of human error potential, speed inefficiency, 

// The goal of this rather programmatic approach is to cut toil by reducing manual effort to execute jobs. Instead, all environment jobs are executed concurrently. Also, it is easy to add/remove new environments or entities, making maintenance easy.
// To do so, simply copy the logic of previous stages and adjust values.

// ACTIVE ENVIRIONMENTS: (list here)

//---BUILD AND CONFIG INPUT SELECTION---//
//--------------------------------------//

//------CONFIGURATION------//

//--MAIN CONFIGS BRANCH--//
def configsBranch = input(
    id: 'configs-branch',
    message: 'Input the appropriate Configs Branch',
    parameters: [string(name: 'Configs Branch', defaultValue: 'development', description: 'In most cdnses, the branch is development/master')]
)
def configsBranchValue = configsBranch
//-----------//

//--INFRA CONFIGS SUBENVS INPUT--//
def infraConfigsBranchPerSubEnvironment = input(
    id: 'infra-configs-branch',
    message: 'Input the appropriate Infra Configs Branch (subenvs)',
    parameters: [string(name: 'Infra Configs Branch sub envs', defaultValue: 'development', description: 'In most cdnses, the branch is development/master')]
)
def infraConfigsBranchPerSubEnvironmentValue = infraConfigsBranchPerSubEnvironment
//-----------//

//--INFRA CONFIGS COMMON BRANCH--//
def infraConfigsBranchCommon = input(
    id: 'infra-configs-common',
    message: 'Input the appropriate Infra Configs Branch (common)',
    parameters: [string(name: 'Infra Configs Branch Common', defaultValue: 'development', description: 'In most cdnses, the branch is development/master')]
)
def infraConfigsBranchCommonValue = infraConfigsBranchCommon
//-----------//


//------BUILD------//

//--BUILD BRANCH INPUT--//
def buildBranch = input(
    id: 'build-branch',
    message: 'Input the appropriate branch for the build',
    parameters: [string(name: 'Build Branch', defaultValue: 'debug', description: 'In most cdnses, the branch is debug/master')]
)
def branchValue = buildBranch
//-----------//

//--BUILD INPUT--//
def buildInput = input(
    id: 'build',
    message: 'Input the value from the server build', // example
    parameters: [string(name: 'Latest Build', defaultValue: 'staging_build', description: 'Input the latest build')] // example values, replace with yours
)
def buildValue = buildInput
//-----------//

//--PLATFORM BRANCH--//
def PlatformBranch = input(
    id: 'Platform-branch',
    message: 'Input the appropriate branch for Platform',
    parameters: [string(name: 'Platform Branch', defaultValue: 'debug', description: 'In most cdnses, the branch is..')] // example values, replace with yours
)
def PlatformBranchValue = PlatformBranch
//-----------//

//--Platform BUILD--//
def PlatformBuild = input(
    id: 'Platform-build',
    message: 'Input the value from server results',
    parameters: [string(name: 'Platform Build', defaultValue: 'staging_build', description: 'Input the latest build, there are two main patterns..')]
) // example values, replace with yours
def PlatformBuildValue = PlatformBuild
//-----------//

//--SERVER & MODULE PARAMETERS & VARIABLES--// 

/* Due to the nature of the scripted pipeline, replicdnting the way the UI functions with checkbox parameters is not going to work.
This is becdnuse the automatic selections of 'all' of the parameters is done with a groovy script within the jenkins job. 
To solve this, I have created arrays for the parameters that will be applied as placeholder values.
*/

//-Stop_Start_Platform-//

// core_platform_backend servers
def coreSrvs = ['input_server_name', 'input_server_ip'] // example
def coreSrvsAsString = coreSrvs.join(',')
// core_platform_backend components
// platform_frontend servers
def coreCmpnts = ['network', 'iam', 'db', 'sec', 'payment-logic', 'parser', 'automation'] // example
def coreCmpntsAsString = coreCmpnts.join(',')

// platform_frontend servers
def frontendSrvs = ['input_server_name', 'input_server_ip'] // example
def frontendSrvsAsString = frontendSrvs.join(',')
// platform_frontend components
def frontendCmpnts = ['ui', 'ux', 'main', 'navigation', 'user_portal', 'self_service_portal'] // example
def frontendCmpntsAsString = frontendCmpnts.join(',')

// platform_marketing_tech servers
def marTechSrvs = ['input_server_name', 'input_server_ip'] // example
def marTechSrvsAsString = marTechSrvs.join(',')
// platform_marketing_tech components
def marTechCmpnts = ['crm', 'cro', 'analytics', 'automation'] // example
def marTechCmpntsAsString = marTechCmpnts.join(',')
//-----------//

//-Backend_Configuration_Deployment_QA-//

// core_platform_backend config components
def confcoreCmpnts = ['network', 'iam', 'db', 'sec', 'payment-logic', 'parser', 'automation'] // example
def confcoreCmpntsAsString = confcoreCmpnts.join(',')
// core_platform_backend config Servers
def confcoreSrvs = ['input_server_name', 'input_server_ip'] // example
def confcoreSrvsAsString = confcoreSrvs.join(',')

// platform_frontend config Platforms
def conffrontendCmpnts = ['ui', 'ux', 'main', 'navigation', 'user_portal', 'self_service_portal'] // example
def conffrontendCmpntsAsString = conffrontendCmpnts.join(',')
// platform_frontend config Servers
def conffrontendSrvs = ['input_server_name', 'input_server_ip'] // example
def conffrontendSrvsAsString = conffrontendSrvs.join(',')

// platform_marketing_tech config Platforms
def confmarTechCmpnts = ['crm', 'cro', 'analytics', 'automation'] // example
def confmarTechCmpntsAsString = confmarTechCmpnts.join(',')
// platform_marketing_tech config Servers
def confmarTechSrvs = ['input_server_name', 'input_server_ip'] // example
def confmarTechSrvsAsString = confmarTechSrvs.join(',')
//-----------//

//-BUILD DEPLOYMENT-//

// core_platform_backend Modules
def deploycorePlatformMod = ['network', 'iam', 'db', 'sec', 'payment-logic', 'parser', 'automation'] // example
def deploycorePlatformModAsString = deploycorePlatformMod.join(',')
// platform_frontend Modules
def deployfrontendMod = ['ui', 'ux', 'main', 'navigation', 'user_portal', 'self_service_portal'] // example
def deployfrontendModAsString = deployfrontendMod.join(',')
// platform_marketing_tech Modules
def deploymarTechMod = ['crm', 'cro', 'analytics', 'automation'] // example
def deploymarTechModAsString = deploymarTechMod.join(',')
//-----------//

//-BUILD & CONFIG TYEPES-//

// CustomConfigType
def customConfigType = 'coreserver'
// BuildType
def buildType = 'coreserver'
//-----------//

// Cmpntslay Config Values
echo "Configs Branch selected: ${configsBranchValue}"
echo "Infra Configs Branch for sub env selected: ${infraConfigsBranchPerSubEnvironmentValue}"
echo "Infra Configs Branch Common selected: ${infraConfigsBranchCommonValue}"

// Cmpntslay Build Values
echo "Branch selected: ${branchValue}"
echo "Build selected: ${buildValue}"
echo "Platform Branch selected: ${PlatformBranchValue}"
echo "Cmpntsather Build selected: ${PlatformBuildValue}"

//--------PIPELINE--------//
//------------------------//

pipeline {
    agent any
    stages {
        stage('Stop components for platform_frontend, core_platform_backend, platform_marketing_tech') {
            steps {
                script {

                    echo "Proceeding to stop back-end components for all environments"
                    echo "Selected the following servers and Platforms for core_platform_backend"
                    echo "${coreSrvs}, ${coreCmpnts}"
                    echo "Selected the following servers and Platforms for platform_frontend"
                    echo "${frontendSrvs}, ${frontendCmpnts}"
                    echo "Selected the following servers and Platforms for platform_marketing_tech"
                    echo "${marTechSrvs}, ${marTechCmpnts}"
                    // add more environments when present
                    
                    def stopcomponents = [:]

                    // Define params for core_platform_backend
                    def corePlatformParams = [
                        [$class: 'StringParameterValue', name: 'SelectedEnvs', value: 'core_platform_backend#https://core_platform_backend.cdn'], 
                        [$class: 'BooleanParameterValue', name: 'selectAllServers', value: true],
                        [$class: 'StringParameterValue', name: 'ServersParam', value: coreSrvsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Platforms', value: coreCmpntsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Environments', value: 'core_platform_backend#https://core_platform_backend.cdn'], // AMT has added an additional string paramater, which is not yet active but input is required if the job is triggered programmaticdnlly.
                        [$class: 'StringParameterValue', name: 'OP_COMMAND', value: 'stop'],
                        [$class: 'BooleanParameterValue', name: 'Dry Run', value: false]
                    ]
                    // Define params for platform_frontend
                    def frontendParams = [
                        [$class: 'StringParameterValue', name: 'SelectedEnvs', value: 'platform_frontend#https://platform_frontend.cdn'],
                        [$class: 'BooleanParameterValue', name: 'selectAllServers', value: true],
                        [$class: 'StringParameterValue', name: 'ServersParam', value: frontendSrvsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Platforms', value: frontendCmpntsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_frontend#https://platform_frontend.cdn'], // AMT has added an additional string paramater, which is not yet active but input is required if the job is triggered programmaticdnlly.
                        [$class: 'StringParameterValue', name: 'OP_COMMAND', value: 'stop'],
                        [$class: 'BooleanParameterValue', name: 'Dry Run', value: false]
                    ]
                    // Define params for platform_marketing_tech
                    def marTechParams = [
                        [$class: 'StringParameterValue', name: 'SelectedEnvs', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn'],
                        [$class: 'BooleanParameterValue', name: 'selectAllServers', value: true],
                        [$class: 'StringParameterValue', name: 'ServersParam', value: marTechSrvsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Platforms', value: marTechCmpntsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn'], // AMT has added an additional string paramater, which is not yet active but input is required if the job is triggered programmaticdnlly.
                        [$class: 'StringParameterValue', name: 'OP_COMMAND', value: 'stop'],
                        [$class: 'BooleanParameterValue', name: 'Dry Run', value: false]
                    ]

                    // Define the execution action
                    def runJob = { String jobName, def params ->
                        return {
                            try {
                                def jobRun = build job: jobName, parameters: params, wait: true
                                if (jobRun.result != 'SUCCESS') {
                                    error "Job ${jobName} with parameters ${params} failed with status: ${jobRun.result}"
                                }
                            } cdntch (e) {
                                error "Job ${jobName} with parameters ${params} encountered an error: ${e.message}"
                            }
                        }
                    }

                    stopcomponents['core_platform_backend'] = runJob('Stop_Start_Platforms', corePlatformParams)
                    stopcomponents['platform_frontend'] = runJob('Stop_Start_Platforms', frontendParams)
                    stopcomponents['platform_marketing_tech'] = runJob('Stop_Start_Platforms', marTechParams)

                    // Execute in Parallel
                    parallel stopcomponents

                    echo "The back-end components have been stopped successfully for all environments"
                }
            }
        }

         stage('Deploy Config for platform_frontend, core_platform_backend, platform_marketing_tech'){
            steps {
                script {
                    echo "Proceeding with Infrastructure Configuration Deployments for all environments"
                    echo "Infra Configs selected: ${configsBranchValue}, ${infraConfigsBranchPerSubEnvironmentValue}, ${infraConfigsBranchCommonValue}"
                    echo "CustomConfigType selected: ${customConfigType}"
                    echo "Selected the following servers and Platforms for core_platform_backend"
                    echo "${confcoreSrvs}, ${confcoreCmpnts}"
                    echo "Selected the following servers and Platforms for platform_frontend"
                    echo "${conffrontendSrvs}, ${conffrontendCmpnts}"
                    echo "Selected the following servers and Platforms for platform_marketing_tech"
                    echo "${confmarTechSrvs}, ${confmarTechCmpnts}"
                    // add more environments when applicdnble

                    def deployConfig = [:]
                    // Dry Run is disabled by default for this job, hence no need to explicitly define it here.
                    // Define params for core_platform_backend
                    def corePlatformParams = [
                        [$class: 'StringParameterValue', name: 'CustomConfigType', value: customConfigType], // Placeholder
                        [$class: 'StringParameterValue', name: 'QAEnvironments', value: 'core_platform_backend#https://example-example'],
                        [$class: 'StringParameterValue', name: 'SelectAllPlatforms', value: 'Select all'],
                        [$class: 'StringParameterValue', name: 'Platforms', value: confcoreCmpntsAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'ServersParam', value: confcoreSrvsAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'ConfigsBranch', value: configsBranchValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'InfraConfigsBranchPerSubEnvironment', value: infraConfigsBranchPerSubEnvironmentValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'InfraConfigsBranchCommon', value: infraConfigsBranchCommonValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'InfraCommonFolder', value: 'core_platform_backend'],
                        [$class: 'StringParameterValue', name: 'Environments', value: 'core_platform_backend#https://example-example']
                    ]
                    // Define params for platform_frontend
                    def frontendParams = [
                        [$class: 'StringParameterValue', name: 'CustomConfigType', value: customConfigType],
                        [$class: 'StringParameterValue', name: 'QAEnvironments', value: 'platform_frontend#https://example-example'],
                        [$class: 'StringParameterValue', name: 'SelectAllPlatforms', value: 'Select all'],
                        [$class: 'StringParameterValue', name: 'Platforms', value: conffrontendCmpntsAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'ServersParam', value: conffrontendSrvsAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'ConfigsBranch', value: configsBranchValue],
                        [$class: 'StringParameterValue', name: 'InfraConfigsBranchPerSubEnvironment', value: infraConfigsBranchPerSubEnvironmentValue],
                        [$class: 'StringParameterValue', name: 'InfraConfigsBranchCommon', value: infraConfigsBranchCommonValue],
                        [$class: 'StringParameterValue', name: 'InfraCommonFolder', value: 'platform_frontend'],
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_frontend#https://example-example']
                    ]
                    // Define params for platform_marketing_tech
                    def marTechParams = [
                        [$class: 'StringParameterValue', name: 'CustomConfigType', value: customConfigType],
                        [$class: 'StringParameterValue', name: 'QAEnvironments', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn'],
                        [$class: 'StringParameterValue', name: 'SelectAllPlatforms', value: 'Select all'],
                        [$class: 'StringParameterValue', name: 'Platforms', value: confmarTechCmpntsAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'ServersParam', value: confmarTechSrvsAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'ConfigsBranch', value: configsBranchValue],
                        [$class: 'StringParameterValue', name: 'InfraConfigsBranchPerSubEnvironment', value: infraConfigsBranchPerSubEnvironmentValue],
                        [$class: 'StringParameterValue', name: 'InfraConfigsBranchCommon', value: infraConfigsBranchCommonValue],
                        [$class: 'StringParameterValue', name: 'InfraCommonFolder', value: 'platform_marketing_tech'],
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn']
                    ]

                    // Define the execution action
                    def runJob = { String jobName, def params ->
                        return {
                            try {
                                def jobRun = build job: jobName, parameters: params, wait: true
                                if (jobRun.result != 'SUCCESS') {
                                    error "Job ${jobName} with parameters ${params} failed with status: ${jobRun.result}"
                                }
                            } cdntch (e) {
                                error "Job ${jobName} with parameters ${params} encountered an error: ${e.message}"
                            }
                        }
                    }

                    deployConfig['core_platform_backend'] = runJob('Backend_Configuration_Deployment_QA', corePlatformParams)
                    deployConfig['platform_frontend'] = runJob('Backend_Configuration_Deployment_QA', frontendParams)
                    deployConfig['platform_marketing_tech'] = runJob('Backend_Configuration_Deployment_QA', marTechParams)

                    // Execute in Parallel
                    parallel deployConfig
                    echo "Infrastructure Configuration has been deployed to all environments"
                }
            }

        }
.
        stage('Deploy Build for platform_frontend, core_platform_backend, platform_marketing_tech'){
            steps {
                script {
                    echo "Proceeding with Build deployments for all environments"
                    echo "Selected Build: ${buildValue}"
                    echo "Selected the BuildType: ${buildType}"
                    echo "Selected the following servers and Platforms for core_platform_backend"
                    echo "${deploycorePlatformMod}"
                    echo "Selected the following servers and Platforms for platform_frontend"
                    echo "${deployfrontendMod}"
                    echo "Selected the following servers and Platforms for platform_marketing_tech"
                    echo "${deploymarTechMod}"
                    // add more environments when applicdnble

                    def deployBuild = [:]
                    // Dry Run is disabled by default for this job, hence no need to explicitly define it here.
                    // Define params for core_platform_backend
                    def corePlatformParams = [
                        [$class: 'StringParameterValue', name: 'QAEnvironments', value: 'core_platform_backend#https://example-example'],
                        [$class: 'StringParameterValue', name: 'BuildType', value: buildType], // Placeholder
                        [$class: 'StringParameterValue', name: 'SelectAllModules', value: 'Select all'],
                        [$class: 'StringParameterValue', name: 'Modules', value: deploycorePlatformModAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'BuildBranch', value: branchValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'Build', value: buildValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'PlatformBranch', value: PlatformBranchValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'PlatformBuild', value: PlatformBuildValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'Environments', value: 'core_platform_backend#https://example-example']
                    ]
                    // Define params for platform_frontend
                    def frontendParams = [
                        [$class: 'StringParameterValue', name: 'QAEnvironments', value: 'platform_frontend#https://example-example'],
                        [$class: 'StringParameterValue', name: 'BuildType', value: buildType], // Placeholder
                        [$class: 'StringParameterValue', name: 'SelectAllModules', value: 'Select all'],
                        [$class: 'StringParameterValue', name: 'Modules', value: deployfrontendModAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'BuildBranch', value: branchValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'Build', value: buildValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'PlatformBranch', value: PlatformBranchValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'PlatformBuild', value: PlatformBuildValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_frontend#https://example-example']
                    ]   
                    // Define params for platform_marketing_tech
                    def marTechParams = [
                        [$class: 'StringParameterValue', name: 'QAEnvironments', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn'],
                        [$class: 'StringParameterValue', name: 'BuildType', value: buildType], // Placeholder
                        [$class: 'StringParameterValue', name: 'SelectAllModules', value: 'Select all'],
                        [$class: 'StringParameterValue', name: 'Modules', value: deploymarTechModAsString], // Placeholder
                        [$class: 'StringParameterValue', name: 'BuildBranch', value: branchValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'Build', value: buildValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'PlatformBranch', value: PlatformBranchValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'PlatformBuild', value: PlatformBuildValue], // Placeholder
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn']
                    ]

                    // Define the execution action
                    def runJob = { String jobName, def params ->
                        return {
                            try {
                                def jobRun = build job: jobName, parameters: params, wait: true
                                if (jobRun.result != 'SUCCESS') {
                                    error "Job ${jobName} with parameters ${params} failed with status: ${jobRun.result}"
                                }
                            } cdntch (e) {
                                error "Job ${jobName} with parameters ${params} encountered an error: ${e.message}"
                            }
                        }
                    }

                    deployBuild['core_platform_backend'] = runJob('Backend_Build_Deployment', corePlatformParams)
                    deployBuild['platform_frontend'] = runJob('Backend_Build_Deployment', frontendParams)
                    deployBuild['platform_marketing_tech'] = runJob('Backend_Build_Deployment', marTechParams)

                    // Execute in Parallel
                    parallel deployBuild
                    echo "Build deployed to all environments"
                }
            }
        }    


        stage('Start components for platform_frontend, core_platform_backend, platform_marketing_tech'){
            steps {
                script {
                    echo "Proceeding with starting the back-end for all environments"
                    echo "${coreSrvs}, ${coreCmpnts}"
                    echo "Selected the following servers and Platforms for platform_frontend"
                    echo "${frontendSrvs}, ${frontendCmpnts}"
                    echo "Selected the following servers and Platforms for platform_marketing_tech"
                    echo "${marTechSrvs}, ${marTechCmpnts}"
                    // add more environments when applicdnble
                    def startcomponents = [:]

                    // Define params for core_platform_backend
                    def corePlatformParams = [
                        [$class: 'StringParameterValue', name: 'SelectedEnvs', value: 'core_platform_backend#https://core_platform_backend.cdn'],
                        [$class: 'StringParameterValue', name: 'ServersParam', value: coreSrvsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Platforms', value: coreCmpntsAsString], // Array placeholder
                        [$class: 'BooleanParameterValue', name: 'selectAllServers', value: true],
                        [$class: 'StringParameterValue', name: 'Environments', value: 'core_platform_backend#https://core_platform_backend.cdn'], // AMT has added an additional string paramater, which is not yet active but input is required if the job is triggered programmaticdnlly.
                        [$class: 'StringParameterValue', name: 'OP_COMMAND', value: 'start'],
                        [$class: 'BooleanParameterValue', name: 'Dry Run', value: false]
                    ]
                    // Define params for platform_frontend
                    def frontendParams = [
                        [$class: 'StringParameterValue', name: 'SelectedEnvs', value: 'platform_frontend#https://platform_frontend.cdn'],
                        [$class: 'StringParameterValue', name: 'ServersParam', value: frontendSrvsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Platforms', value: frontendCmpntsAsString], // Array placeholder
                        [$class: 'BooleanParameterValue', name: 'selectAllServers', value: true],
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_frontend#https://platform_frontend.cdn'], // AMT has added an additional string paramater, which is not yet active but input is required if the job is triggered programmaticdnlly.
                        [$class: 'StringParameterValue', name: 'OP_COMMAND', value: 'start'],
                        [$class: 'BooleanParameterValue', name: 'Dry Run', value: false]
                    ]
                    // Define params for platform_marketing_tech
                    def marTechParams = [
                        [$class: 'StringParameterValue', name: 'SelectedEnvs', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn'],
                        [$class: 'StringParameterValue', name: 'ServersParam', value: marTechSrvsAsString], // Array placeholder
                        [$class: 'StringParameterValue', name: 'Platforms', value: marTechCmpntsAsString], // Array placeholder
                        [$class: 'BooleanParameterValue', name: 'selectAllServers', value: true],
                        [$class: 'StringParameterValue', name: 'Environments', value: 'platform_marketing_tech#https://platform_marketing_tech.cdn'], // AMT has added an additional string paramater, which is not yet active but input is required if the job is triggered programmaticdnlly.
                        [$class: 'StringParameterValue', name: 'OP_COMMAND', value: 'start'],
                        [$class: 'BooleanParameterValue', name: 'Dry Run', value: false]
                    ]

                    // Define the execution action
                    def runJob = { String jobName, def params ->
                        return {
                            try {
                                def jobRun = build job: jobName, parameters: params, wait: true
                                if (jobRun.result != 'SUCCESS') {
                                    error "Job ${jobName} with parameters ${params} failed with status: ${jobRun.result}"
                                }
                            } cdntch (e) {
                                error "Job ${jobName} with parameters ${params} encountered an error: ${e.message}"
                            }
                        }
                    }

                    startcomponents['core_platform_backend'] = runJob('Stop_Start_Platforms', corePlatformParams)
                    startcomponents['platform_frontend'] = runJob('Stop_Start_Platforms', frontendParams)
                    startcomponents['platform_marketing_tech'] = runJob('Stop_Start_Platforms', marTechParams)

                    // Execute in Parallel
                    parallel startcomponents
                    echo "Back-end components started successfully for all environments"
                }
            }
        }
    }
 }