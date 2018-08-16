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

  CommonW=$TmuxSessionName:1
  new-window-splitwh-setup $CommonW 'modules/common' 'Common';
  select-pane $CommonW -L;
  send-to $CommonW \
      '../..' Enter \
      'sbt' Enter \
      'project common' Enter \
  ;
  select-pane $CommonW -R;

  FirstW=$TmuxSessionName:2
  new-window-splitwh-setup $FirstW 'modules/first' 'First';
  select-pane $FirstW -L;
  send-to $FirstW \
      '../..' Enter \
      'sbt' Enter \
      'project first' Enter \
  ;
  select-pane $FirstW -R;

  SecondW=$TmuxSessionName:4
  new-window-splitwh-setup $SecondW 'modules/second' 'Second';
  select-pane $SecondW -L;
  send-to $SecondW \
      '../..' Enter \
      'sbt' Enter \
      'project second' Enter \
  ;
  select-pane $SecondW -R;

  select-window $MainW;
}

if ! is-initiated; then
  initiate-tmux
fi

attach-tmux
