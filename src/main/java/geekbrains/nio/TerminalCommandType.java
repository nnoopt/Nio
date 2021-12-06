package geekbrains.nio;

import java.util.Arrays;

public enum TerminalCommandType {
    LS ("ls"),
    MKDIR ("mkdir"),
    CD ("cd"),
    CAT ("cat"),
    TOUCH ("touch");

    private final String command;

    TerminalCommandType(String command) {
        this.command = command;
    }

    public String getCommand (){
        return command;
    }

    public static TerminalCommandType byCommand (String command){
        return Arrays.stream(values()).
                filter(cmd->cmd.getCommand().equals(command))
                .findAny()
                .orElseThrow(()->new RuntimeException("Command not found"));
    }
}
