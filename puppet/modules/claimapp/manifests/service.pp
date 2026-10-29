# Keeps the application running and checks its health after every start or restart.
class claimapp::service {
  $ctl = "${claimapp::base_dir}/bin/claim-surveyor.sh"

  # The node is a container without systemd, so the service is driven by our own control script.
  service { 'claim-surveyor':
    ensure     => running,
    provider   => 'base',
    start      => "${ctl} start",
    stop       => "${ctl} stop",
    restart    => "${ctl} restart",
    status     => "${ctl} status",
    hasstatus  => true,
    hasrestart => true,
  }

  # Runs only when the service was started or restarted (so a second run changes nothing).
  exec { 'claim-surveyor-health-check':
    command     => "curl -fsS --retry 40 --retry-delay 3 --retry-connrefused http://localhost:${claimapp::port}/actuator/health",
    path        => ['/usr/bin', '/bin'],
    refreshonly => true,
    logoutput   => true,
    timeout     => 300,
    subscribe   => Service['claim-surveyor'],
  }
}
