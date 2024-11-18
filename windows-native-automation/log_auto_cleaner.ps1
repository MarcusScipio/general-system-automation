###---DRIVE SELF CLEANING MECHANISM---###

<# 
Author: Marcus Scipio

This script is to be used with a windows task-scheduler job. 
The script (should) run at certain time intervals to scan the drive size and clean it of any "junk" logs that can fill up the space, preventing machines from crashing.

Mare sure to replace "example" with proper naming, this is for the sake of this example only.
#>

$driveLetter = "C"         # Replace with the drive letter you want to monitor
$intervalInSeconds = 120    # Interval between checks, in seconds
$thresholdPercentage = 2    # Threshold percentage for low free space

function DriveClean {
    param (
        [string]$Path = "path\to\",
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
    $Path = "C:\example" # example path

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


# while ($true) {
    # Get the volume information using Get-Volume cmdlet
    $volume = Get-Volume -DriveLetter $driveLetter

    $freeSpace = $volume.SizeRemaining / 1GB   # Convert to gigabytes
    $totalSpace = $volume.Size / 1GB           # Convert to gigabytes

    if ($totalSpace -eq 0) {
        Write-Host "Total space for drive $driveLetter is zero. Skipping calculation."
    } else {
        $usedSpace = $totalSpace - $freeSpace
        $usedPercentage = ($usedSpace / $totalSpace) * 100
        $freeSpacePercentage = 100 - $usedPercentage

        $currentDate = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

        Write-Host "$currentDate - Drive $driveLetter - Total Space: $($totalSpace) GB, Used Space: $($usedSpace) GB, Free Space: $($freeSpace) GB, Used Percentage: $($usedPercentage)%"

        if ($freeSpacePercentage -le $thresholdPercentage) {
            Write-Host "Warning: Free space on drive $driveLetter is below the $($thresholdPercentage)% threshold. Initiating clean-up"
            # Call the functions 
            DriveClean -Hostname $env:COMPUTERNAME
            ExampleLogs -Hostname $env:COMPUTERNAME  

            Write-Host "Log files that are currently used by the system will not be deleted and printed as error messages. Please ignore"
            
            # Add any actions you want to perform when the free space is below the threshold
        }
        else {
            Write-Host "Free space is currently above threshold. Skipping clean-up."
        }
    }

# }
