###---DRIVE MANUAL CLEANER---###

<# 
Author: Marcus Scipio

The script automates the manual action of cleaning up "filler" logs in a machine drive / folder.
Simply execute the script and input the targeted machine in the command line, you will be prompted.

Replace the "example" with proper naming according to your set-up
#>

    
# Authenticate with your active directory username, password or use the QA-OPS service:

$username = # user
$pass = # pass
$encryptedCred = ConvertTo-SecureString -AsPlainText -String $pass -Force
$cred = New-Object System.Management.Automation.PSCredential($username, $encryptedCred)
$option = New-PSSessionOption -SkipCACheck -SkipCNCheck
$machine = Read-Host -input "input the targeted machine for a cleanup
"
$s = New-PSSession -ComputerName $machine -Credential $cred -SessionOption $option


Invoke-Command -Session $s -ScriptBlock {

                function DriveClean {
                    param (
                        [string]$Path = "C:\example\logs",
                        [string]$Hostname,
                        [double]$Threshold = 100
                
                    )
                
                    # Get the initial size of the directory
                    $BeforeSize = (Get-ChildItem -Path $Path -Recurse | Measure-Object -Property Length -Sum).Sum
                    Write-Host "Directory size before cleaning: $($BeforeSize / 1MB) MB"
                
                    # Find and delete any logs that exceed the threshold
                    Get-ChildItem -Path $Path -Recurse -File | ForEach-Object {
                        if ($_.Length -gt ($Threshold * 1MB)) {
                            Write-Host "Deleting log $($_.FullName) because it is larger than $Threshold MB"
                            Remove-Item $_.FullName
                        }   
                    }
                
                    # Get the final size of the directory
                    $AfterSize = (Get-ChildItem -Path $Path -Recurse | Measure-Object -Property Length -Sum).Sum
                    Write-Host "Directory size after cleaning on ${Hostname}: $($AfterSize / 1MB) MB"
                }
                
                

                # Define function to check Example logs
                function ExampleLogs {    
                    param (
                        [string]$Hostname
                    )
                    # Set the path to the logs directory
                    $Path = "C:\logs" # example path
                
                    # Get the date for today
                    $Today = Get-Date -Format "yyyyMMdd"
                
                    # Check if an older Example log file exists
                    $OlderExampleLog = Get-ChildItem -Path $Path -Filter "Example.log.2*" | Where-Object { $_.Name -notmatch $Today }
                
                    if ($OlderExampleLog) {
                        # Evaluate the size of the older Example log file
                        $OlderLogSizeMB = [math]::Round(($OlderExampleLog.Length / 1MB), 2)
                
                        # Check if the size is over 500 MB and delete the file
                        if ($OlderLogSizeMB -gt 100) {
                            Write-Host "Deleting older Example log $($OlderExampleLog.Name) because it is larger than 500 MB for $item" 
                            Remove-Item -Path $OlderExampleLog.FullName 
                        } else {
                            Write-Host "Older Example log $($OlderExampleLog.Name) is smaller than 500 MB for $item"
                        }
                    } else {
                        Write-Host "No older Example log file exists currently in ${Hostname}."
                    }
                }
                

                # Call the functions 
                DriveClean -Hostname $env:COMPUTERNAME
                ExampleLogs -Hostname $env:COMPUTERNAME

                Write-Host "Log files that are currently used by the system will not be deleted and printed as error messages. Please ignore"

    }
    Write-Host "Cleaning finished-up"

