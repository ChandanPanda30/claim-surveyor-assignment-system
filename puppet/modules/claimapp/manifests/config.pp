# Runtime configuration file and the start/stop script, both generated from templates.
class claimapp::config {
  file { '/etc/claim-surveyor/claim-surveyor.env':
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    content => epp('claimapp/claim-surveyor.env.epp', {
      'port'      => $claimapp::port,
      'base_dir'  => $claimapp::base_dir,
      'java_opts' => $claimapp::java_opts,
    }),
  }

  file { "${claimapp::base_dir}/bin/claim-surveyor.sh":
    ensure  => file,
    owner   => 'root',
    group   => 'root',
    mode    => '0755',
    content => epp('claimapp/claim-surveyor.sh.epp', {
      'app_user' => $claimapp::app_user,
      'base_dir' => $claimapp::base_dir,
    }),
  }
}
