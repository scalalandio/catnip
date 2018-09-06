#!/bin/zsh

source "${0:a:h}/.common.sh"

function initiate-tmux() {
  # Initiate with TmuxSessionName
  new-session;

  MainW=$TmuxSessionName:0
  set-option $MainW allow-rename off > /dev/null;
  splitwh-setup $MainW '.' \
      'sbt' \
      'git status' \
  ;
  select-pane $MainW -L; send-to $MainW Enter;
  select-pane $MainW -R; send-to $MainW Enter;
  rename-window $MainW 'root';

  CatnipJVMW=$TmuxSessionName:1
  new-window-splitwh-setup $CatnipJVMW 'modules/catnipJVM' 'CatnipJVM';
  select-pane $CatnipJVMW -L;
  send-to $CatnipJVMW \
      '../..' Enter \
      'sbt' Enter \
      'project catnipJVM' Enter \
  ;
  select-pane $CatnipJVMW -R;

  CatnipJS=$TmuxSessionName:2
  new-window-splitwh-setup CatnipJS 'modules/catnipJS' 'CatnipJS';
  select-pane $CatnipJS -L;
  send-to $CatnipJS \
      '../..' Enter \
      'sbt' Enter \
      'project catnipJS' Enter \
  ;
  select-pane $CatnipJS -R;

  select-window $MainW;
}

if ! is-initiated; then
  initiate-tmux
fi

attach-tmux
