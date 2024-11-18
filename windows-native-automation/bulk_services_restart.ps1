<# Author: Marin Govedarski

Purpose: Automate the killing of hanging processes across a wide array of hosts. It saves a ton of time.
#>

# Auto autheticate:
$username = ""
$pass = ""
$encryptedCred = ConvertTo-SecureString -AsPlainText -String $pass -Force

# Create a PSCredential object using the decrypted credentials
$cred = New-Object System.Management.Automation.PSCredential($username, $encryptedCred)

# Define an array containing all relevant servers. For such a small and simple script, using a separate txt file is not necessary.

<# 
You can use host.txt for reference. 
CURRENT: STAGING

#>

$srvs = @("10.10.10.10, 10.11.11.11") # all relevant hosts by ip or name

$option = New-PSSessionOption -SkipCACheck -SkipCNCheck
$sessions = New-PSSession -ComputerName $srvs -Credential $cred -SessionOption $option

# Loop through each host in the array
foreach ($s in $sessions) {

     Invoke-Command -Session $s -ScriptBlock {

     Write-Host "Attempting to stop service $($service.Name) on $env:COMPUTERNAME"

     # Retrieve all services with the string pattern "SMD"
     $services = Get-Service | Where-Object {$_.Name -like "*SMD*"} # example name

     # Loop through each service found and attempt to stop it
     foreach ($service in $services) {
     try {
          $service | Stop-Service  -Force -ErrorAction Stop # Start-Service to start all services instead. In that case, remove the "-Force"
          Write-Host "Successfully stopped service: $($service.Name)"
          Write-Host "Host: $($s.Name)"
     } catch {
          Write-Warning "Failed to stop service: $($service.Name). Error: $($_.Exception.Message)"
     }

    }
  }
} 