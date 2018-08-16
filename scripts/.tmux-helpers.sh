# Define following variables/functions:
# - $TmuxSessionName - name of created session
# - $TmuxInitDir - base directory for new windows/panes, current dir by default
# - $TmuxCommons - file to source in startup, common.sh by default

TmuxInitDir=${TmuxInitDir:-"${0:a:h}"}
TmuxCommons=${TmuxCommons:-"${0:a:h}/.common.sh"}

alias \
    is-initiated="tmux has-session -t '$TmuxSessionName' &> /dev/null" \
    new-session="tmux new -s '$TmuxSessionName' -c '$TmuxInitDir' -n '$TmuxSessionName' -d" \
    kill-session="tmux kill-session -t '$TmuxSessionName'" \
    attach-tmux="tmux attach -t '$TmuxSessionName'" \
    detach-tmux="tmux detach" \
    select-window='tmux select-window -t' \
    select-pane='tmux select-pane -t' \
    set-option='tmux set-option -t' \
    rename-window="tmux rename-window -t" \
    send-to='tmux send -t' \
    splitwh='tmux splitw -h -t' \
;

function new-window() {
  tmux neww -t $1 -c "$TmuxInitDir/$2" -n $3;
  setup-window $1;
}

function setup-window() {
  set-option $1 allow-rename off > /dev/null;
}

function setup-active-pane() {
  send-to $1 \
      "export TmuxSessionName='$TmuxSessionName'" Enter \
      "source '$TmuxCommons'" Enter \
      '/usr/bin/clear' Enter \
  ;
}

function splitwh-setup() {
    setup-active-pane $1;
    send-to $1 $3;
  splitwh $1 -c "$TmuxInitDir/$2";
    setup-active-pane $1;
    send-to $1 $4;
}

function new-window-splitwh-setup() {
  new-window $1 $2 $3;
  splitwh-setup $1 $2 $4 $5;
  rename-window $1 $3;
}

alias tmux-helpers-help="echo 'Available tmux helpers commands:
is-initiated - checks whether $TmuxSessionName session is initiated
new-session  - creates new session named $TmuxSessionName
kill-session - kills session named $TmuxSessionName
attach-tmux  - attaches terminal to $TmuxSessionName session
detach-tmux  - detaches terminal from $TmuxSessionName session

new-window $TmuxSessionName:no subdir window-name - creates new window in subdir and gives it a name
setup-window $TmuxSessionName:no                  - applies some setup to window
select-window $TmuxSessionName:no                 - selects no as active window
rename-window $TmuxSessionName:no new-name        - renames window to new-name

select-pane $TmuxSessionName:no -L/-R             - selects left/right pane
setup-active-pane $TmuxSessionName:no             - applies some setup to active pane

set-option $TmuxSessionName:no                    - set option
send-to $TmuxSessionName:no command1 command2...  - sens commands to window no
splitwh $TmuxSessionName:no                       - splits horizontally window no

splitwh-setup $TmuxSessionName:no subdir left-pane-command right-pane-command
                                                  - splits window, sets it up and sends initial commands to panes
new-window-splitwh-setup $TmuxSessionName:no subdir window-name left-pane-command right-pane-command
                                                  - creates new window, splits it, sets it up and initializes panes with commands
'"
