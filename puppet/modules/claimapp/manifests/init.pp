# Puppet module "claimapp": everything a Linux node needs to run the Claim Surveyor Assignment System.
#
# Layout on the node
#   /opt/claim-surveyor/releases/<version>/claim-surveyor.war   one folder per release (kept for rollback)
#   /opt/claim-surveyor/current -> releases/<version>           symlink switched on deploy / rollback
#   /opt/claim-surveyor/data                                    H2 database (survives releases)
#   /opt/claim-surveyor/logs, run, bin                          log file, pid file, start/stop script
#   /etc/claim-surveyor/claim-surveyor.env                      runtime configuration (PORT, DB_PATH, JAVA_OPTS)
class claimapp (
  String  $version      = '1.0.0',
  Integer $port         = 8080,
  String  $app_user     = 'claimapp',
  String  $base_dir     = '/opt/claim-surveyor',
  String  $artifact_dir = '/puppet/artifacts',
  String  $java_opts    = '-Xms128m -Xmx384m',
) {
  contain claimapp::install
  contain claimapp::config
  contain claimapp::service

  Class['claimapp::install'] -> Class['claimapp::config'] ~> Class['claimapp::service']

  # Switching the "current" release (deploy or rollback) restarts the application.
  File["${base_dir}/current"] ~> Service['claim-surveyor']
}
