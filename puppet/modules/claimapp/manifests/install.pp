# Packages, service account, folders and the versioned release.
class claimapp::install {
  $user         = $claimapp::app_user
  $base         = $claimapp::base_dir
  $version      = $claimapp::version
  $artifact_dir = $claimapp::artifact_dir

  # --- packages ---
  package { ['openjdk-21-jre-headless', 'curl', 'ca-certificates']:
    ensure => installed,
  }

  # --- service account (no login shell) ---
  group { $user:
    ensure => present,
    system => true,
  }

  user { $user:
    ensure     => present,
    gid        => $user,
    system     => true,
    home       => $base,
    managehome => false,
    shell      => '/usr/sbin/nologin',
    comment    => 'Claim Surveyor service account',
    require    => Group[$user],
  }

  # --- folders ---
  file { [$base, "${base}/releases", "${base}/bin", '/etc/claim-surveyor']:
    ensure => directory,
    owner  => 'root',
    group  => 'root',
    mode   => '0755',
  }

  file { ["${base}/data", "${base}/logs", "${base}/run"]:
    ensure  => directory,
    owner   => $user,
    group   => $user,
    mode    => '0750',
    require => [User[$user], File[$base]],
  }

  # --- the release: WAR copied from the artifact folder, then the "current" link is switched ---
  file { "${base}/releases/${version}":
    ensure  => directory,
    owner   => 'root',
    group   => 'root',
    mode    => '0755',
    require => File["${base}/releases"],
  }

  file { "${base}/releases/${version}/claim-surveyor.war":
    ensure  => file,
    source  => "${artifact_dir}/claim-surveyor-${version}.war",
    owner   => 'root',
    group   => 'root',
    mode    => '0644',
    require => File["${base}/releases/${version}"],
  }

  file { "${base}/current":
    ensure  => link,
    target  => "${base}/releases/${version}",
    require => File["${base}/releases/${version}/claim-surveyor.war"],
  }
}
