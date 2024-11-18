###---REMOTE TASK SCHEDULER---###

<# 
Author: Marcus Scipio

Part of the task-scheduler remote administration workflow.
Use this script to create new windows task-scheduler jobs remotely.
Adjust the script accordingly: server names/ip's, job details, etc.
#>

# OPTIONAL: 

# $srvs = gc D:\temp\list2.txt

# $srvs = $srvs.Split(", `r`n", [System.StringSplitOptions]::RemoveEmptyEntries)
# if ($srvs.count -eq 0) {exit}
# $srvs.Count
    
# Authenticate with your active directory username, password or use the QA-OPS service:
$username = # user
$pass = # pass
 $encryptedCred = ConvertTo-SecureString -AsPlainText -String $pass -Force

 # Create a PSCredential object using the decrypted credentials
 $cred = New-Object System.Management.Automation.PSCredential($username, $encryptedCred) 

    
$srvs=@"
10.30.154.3
"@ -split "`r`n"
$srvs.Count

$option = New-PSSessionOption -SkipCACheck -SkipCNCheck
$sessions = New-PSSession -ComputerName $srvs -Credential $cred -SessionOption $option

foreach ($s in $sessions) {

        # Create the scheduled task on the remote machine
        Invoke-Command -Session $s -ScriptBlock {
            $taskName = "Drive-Self-Cleaningscr"
            $taskCommand = "C:\tools\drive-self-checker.ps1"
            $taskArguments = "-arg1 self-cleaning-mechanism to prevent storage issues"
            $startupTrigger = New-ScheduledTaskTrigger -AtStartup
            $repeatingTrigger = New-ScheduledTaskTrigger -Once -At (Get-Date) -RepetitionInterval (New-TimeSpan -Minutes 30)
            $taskSettings = New-ScheduledTaskSettingsSet
            $taskPrincipal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount
            $taskAction = New-ScheduledTaskAction -Execute $taskCommand -Argument $taskArguments
            # Import the ScheduledTasks module if it is not already imported
            if (-not (Get-Module ScheduledTasks)) {
                Import-Module ScheduledTasks
            }

            # Check if the task already exists and delete it if it does
            $taskName = $using:taskName
            Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue

            # Create the new scheduled task
            Register-ScheduledTask -TaskName $taskName -Trigger $using:startupTrigger, $using:repeatingTrigger -Settings $using:taskSettings -Principal $using:taskPrincipal -Action $using:taskAction
        }

        # Verify that the task has been created on the remote machine
        $taskExists = Invoke-Command -Session $s -ScriptBlock {
            # Check if the task exists
            $taskName = $using:taskName
            $task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
            if ($task -eq $null) {
                return $false
            }

            # Check if the task trigger is correct
            $taskTrigger = $task.Triggers[0]
            if ($taskTrigger.RepetitionInterval.TotalMinutes -ne 30 -or $taskTrigger.RepetitionDuration -ne [TimeSpan]::MaxValue) {
                return $false
            }

            # Check if the task action is correct
            $taskAction = $task.Actions[0]
            if ($taskAction.Arguments -ne $using:taskArguments -or $taskAction.Path -ne $using:taskCommand) {
                return $false
            }

            # If all checks pass, return true
            return $true
        }

        # Output the verification result
        if ($taskExists) {
            Write-Host "Scheduled task '$taskName' has been successfully created on $s"
        } else {
            Write-Warning "Failed to create scheduled task '$taskName' on $s"
        }
  }

Remove-PSSession -Session $sessions