###---CUSTOM FILE RETRIEVER & VERSIONER---###

<#
Author: Marcus Scipio

ACTION SEQUENCE: 
1. Create a back-up of the current (respective) files.
2. Download the files from artifactory.
3. Process the files, check versioning and handle errors.
4. Deploy files to relevant paths.
5. Initiate the re-reading operation and fallback if necessary.
6. Generate an e-mail notification.
#>

# AWS SECRET PREPARATION:
Install-Module -Name AWSPowerShell.NetCore -Force
# The ec2 instance should an assigned role to access AWS secrets.
# The ARN or name of your secret
$secretId = "arn:aws:secretsmanager:region:account-id:secret:your-secret-name" # example
# Retrieve the secret
$secretValue = Get-SECSecretValue -SecretId $secretId
# Directly use the SecretString property since it's a simple one-liner API key
$artifactoryAuthToken = $secretValue.SecretString

Write-Host "Retrieved API Token: $artifactoryAuth"

# PATH VARIABLES
# TLS protocol definition is only necessary if the PS version is conflicting.
# [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$artifactoryFilePath = "path/to/artifactory" # The main destination for artifactory downloads, to be determined
$ip2FilesSourcePath = "C:\path\*" # Source path of the current files, to be determined
$backUpPath = "C:\path\back-up" # Path to backup files in case of a rollback, to be determined
# PATH PROCESSING: 
$downloadedArtifactFilePath = [System.IO.Path]::GetFullPath("D:\retriever-tester\downloaded-artifact")
$fileName = [System.IO.Path]::GetFileName($artifactoryFilePath)
$processedArtifactFilePath = [System.IO.Path]::Combine($downloadedArtifactFilePath, $fileName)
$mainDeploymentPath = "C:\retriever" # Lobby machine path/s, to be determined
$currentIncVersionPath = "C:\path\version.inc" # Source path to the current .inc file, to be determined
$newIncVersionPath = "C:\path\downloaded-artifact" # Source path to the new .inc file, to be determined

# LOGGING
$date_log = Get-Date -Format "yyyyMMdd"
$logPath = "./" # To be determined

# Create a back-up in case of a rollback.
function BackUp-Init {

     Copy-Item -Path $ip2FilesSourcePath -Destination $backUpPath -Recurse -Force
     Write-Host "Backup completed from $ip2SourcePath to $backUpPath"
}

# Download the files from Artifactory:
function Get-Artifacts {
     try {
          Invoke-WebRequest -Uri $artifactoryFilePath -OutFile $processedArtifactFilePath -Headers @{Authorization = "Bearer $artifactoryAuthToken"}
          # Invoke-WebRequest -Uri $artifactoryFilePath -OutFile $downloadedArtifactFilePath -Headers @{ "APIKey" = "${artifactoryAuthToken}"}

     }
     catch {
          Write-Error "Failed to download the files from Artifactory. Error: $_"
     }
}
     
# Extract the ZIP files:
function Expand-ArchivedFiles {
     # Nested function to get versioning.
     function Get-VersionFromInc {

          $content = Get-Content -Path $filePath -Raw -ErrorAction SilentlyContinue
          $versionRegex = 'Schema-version=(\d+\.\d+).*?Updated=(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}-\d{2}:\d{2})'
          $regexMatch = [regex]::Match($content, $versionRegex)

          if ($matches.Success) {
               return @{
               SchemaVersion = $regexMatch.Groups[1].Value
               UpdatedTimestamp = $regexMatch.Groups[2].Value
               }
          }
          else {
               Write-Error "Version information not found in $filePath"
               return $null
          }
     }
     # Test the archive, then test if it was extracted properly.
     if (-Not (Test-Path -Path $downloadedArtifactFilePath)) {
          Write-Error "ZIP file does not exist"
          return
     }

     try {
          Expand-Archive -Path "${downloadedArtifactFilePath}\retriever.zip" -DestinationPath $downloadedArtifactFilePath -Force
          Write-Host "Successfully extracted ZIP file to: $downloadedArtifactFilePath"
     } catch {
          Write-Error "Failed to extract ZIP file: $_"
     }

     $retrieverIncPath = "${downloadedArtifactFilePath}\retriever.inc"

     if (-not (Test-Path $retrieverIncPath)) {
          $errorMessage = "The file doesn't exist"
          Write-Host $errorMessage
          $errorMessage | Out-File "${logPath}\script_result.${date_log}" -Append
          return
     }
     # Initate the check file version logic
     # Call the nested funcion

     $currentInfo = Get-VersionFromInc -filePath $currentIncVersionPath
     $newInfo = Get-VersionFromInc -filePath $newIncVersionPath

     # OPTIONAL: 

     # if ($null -eq $currentInfo -or $null -eq $newInfo) {
     #      Write-Error "Unable to obtain version information from one or both files."
     #      exit
     #  }
     
     if ($currentInfo.SchemaVersion -eq $newInfo.SchemaVersion -and $currentInfo.UpdatedTimestamp -eq $newInfo.UpdatedTimestamp) {
          exit
     }
     else {
          Write-Host "A new update is present, the script will continue."
          # Continue with script execution.
     }
}

# Distribute the relevant files to all possible destinations.
function Deploy-Files {
     try {
     #  Remove-Item -Path "${downloadedArtifactFilePath}\retriever.zip"
          Copy-Item -Path "${downloadedArtifactFilePath}\path\*" -Destination $mainDeploymentPath -Recurse -Force -ErrorAction Stop
          Write-Host "Files deployed to lobby successfully."
     } catch {
          Write-Error "Error copying files: $_"
     }      
}

function Get-retriever {
     # Usage of local params is more efficient for this function. We are omitting the need of an .ini file.
     param (
     # tested on qa_release. This will adjusted with the proper machine in sts-coretech.
     $exePath = "\\10.10.10.10\d$\path\pyrgeoiprereadsmem.exe", # example
     $serverIp = "10.10.10:2348", # example
     $instance = "exampleInstance", # example
     $connType = "exampleAuth" # example
     )

     try {
     # Verify retriever.inc existence
     $retrieverIncPath = Join-Path -Path $mainDeploymentPath -ChildPath "retriever.inc"
     if (-not (Test-Path $retrieverIncPath)) {
          $errorMessage = "The file ${retrieverIncPath} doesn't exist"
          $errorMessage | Out-File "${logPath}\script_result.${date_log}" -Append
          throw $errorMessage
     }
     # Execute pyrgeoiprereadsmem.exe and log in case of errors.
     $executable = Start-Process -FilePath $exePath -ArgumentList "$serverIp $instance $connType" -NoNewWindow -PassThru -ErrorAction SilentlyContinue
     if ($executable.ExitCode -ne 0) {
          $errorMsg = "Execution of ${exePath} failed with exit code $($executable.ExitCode)" 
          $errorMsg | Out-File "${logPath}\script_result_Errors1.${date_log}" -Append
          throw $errorMsg
     }

     Write-Host "pyrgeoiprereadsmem.exe executed successfully: ${exePath}"
     }
     catch {
     Write-Host "Error encountered: $_. Initiating a rollback.."

     # Rollback: Revert to the backup
     Copy-Item -Path $backupPath\* -Destination $mainDeploymentPath -Recurse -Force
     Write-Host "Fallback completed: Reverted changes from backup to $mainDeploymentPath"

     # Rereading operation again:
     $executable = Start-Process -FilePath $exePath -ArgumentList "$serverIp $instance $connType" -NoNewWindow -PassThru -ErrorAction SilentlyContinue
     if ($executable.ExitCode -ne 0) {
          throw "Execution of ${exePath} failed with exit code $($executable.ExitCode)" | Out-File "${logPath}\script_result_Errors1.${date_log}" -Append
     }

     Write-Host "Second reread executed successfully, rollback completed"
     
     # Optionally, log the error or take additional error handling steps here
     }
}

# Create an email notification. Upon proper incorporating proper monitoring, this function might be redundant.
function Send-toEmail {
     # Global vars are not used here, so we define local params.
     param (
     $Subject = "script issues",
     $Body = "Testing new scripts",
     $smtpHost = "10.10.10.10",
     $smtpFrom = "example-mail",
     $alertEmailTo = "example-mail"
     )

     Send-MailMessage -SmtpServer $smtpHost -From $smtpFrom -To $alertEmailTo -Subject $Subject -Body $Body
     Write-Host "Notifications sent successfully."
}

###---/FUNCTION EXECUTION IN ORDER/---###

BackUp-Init
Get-Artifacts
Expand-ArchivedFiles # contains nested functions
Deploy-Files
Get-retriever
Send-toEmail