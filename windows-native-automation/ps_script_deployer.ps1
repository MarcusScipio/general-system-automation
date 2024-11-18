###---SCRIPT DEPLOYER---###

<# 
Author: Marcus Scipio

This script acts as a "custom" file deployment mechanism to a number of windows vm's across your environments simultaneously.
#>

    
# Authenticate with your active directory username, password:
$username = # user
$pass = # pass
$encryptedCred = ConvertTo-SecureString -AsPlainText -String $pass -Force

$cred = New-Object System.Management.Automation.PSCredential($username, $encryptedCred)

    # Array of remote hosts (ip's or machine names)
    $remoteHosts = @(
        # Examples:
        "10.178.33.221",
        "10.178.20.201"
        )
    # Define the local and remote script paths
    $localScriptPath = ".\drive-self-cleaner.ps1"
    $remoteScriptPath = "C:\tools"

    # Loop through each remote host
    foreach ($remoteHost in $remoteHosts) {
        # Open a session with the remote host
        try {
            $session = New-PSSession -ComputerName $remoteHost -Credential $cred
        } catch {
            Write-Host "Failed to create a session with $remoteHost. Error: $_"
            continue
        }

        # Copy the script to the remote host
        try {
            Copy-Item -Path $localScriptPath -Destination $remoteScriptPath -ToSession $session
        } catch {
            Write-Host "Failed to copy script to $remoteHost. Error: $_"
        } finally {
            # Close the remote session
            Remove-PSSession -Session $session
        }
    }
