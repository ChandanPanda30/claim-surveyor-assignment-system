# Entry point: configures this node to run the Claim Surveyor Assignment System.
#
# The release to run is chosen with the external fact "release_version":
#   FACTER_release_version=1.1.0 puppet apply site.pp ...
# Re-applying with an older version is the rollback procedure.

$release = $facts['release_version'] ? {
  undef   => '1.0.0',
  default => $facts['release_version'],
}

class { 'claimapp':
  version => $release,
}
