{ pkgs, ... }: {
  channel = "stable-24.05";
  packages = [
    pkgs.jdk17
    pkgs.unzip
  ];

  env = {
    ANDROID_HOME = "$HOME/Android/Sdk";
    ANDROID_SDK_ROOT = "$HOME/Android/Sdk";
    sdkmanager = "$HOME/Android/Sdk/cmdline-tools/latest/bin/sdkmanager";
  };

  idx = {
    extensions = [

    ];

    previews = {
      enable = true;
      previews = {

      };
    };

    workspace = {

      onCreate = {

      };

      onStart = {

      };
    };
  };
}
