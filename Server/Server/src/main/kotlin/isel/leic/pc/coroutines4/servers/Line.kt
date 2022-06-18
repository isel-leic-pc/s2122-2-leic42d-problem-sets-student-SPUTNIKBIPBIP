package isel.leic.pc.coroutines4.servers

open class Line {
    private constructor() {}

    class Message(val value : String) : Line()

    class EnterRoomCommand (val name : String) : Line()

    class ShutdownCommand (val timeout : Long) : Line()

    class LeaveRoomCommand   : Line()

    class ExitCommand  : Line()

    class InvalidLine(val reason: String) : Line()

    companion object {

        fun parse(line : String) : Line {
            if (!line.startsWith("/"))
            {
                return Message(line);
            }

            var parts = line.split(" ");
            return when(parts[0]) {
                "/enter" -> parseEnterRoom(parts)
                "/leave" -> parseLeaveRoom(parts)
                "/exit" -> parseExit(parts)
                "/shutdown" -> parseShutdown(parts)
                else->  InvalidLine("Unknown command")
            }
        }

        private fun parseEnterRoom(parts: List<String>): Line {
            return if (parts.size != 2) {
                InvalidLine("/enter command requires exactly one argument")
            } else EnterRoomCommand(parts[1])
        }

        private fun parseLeaveRoom(parts: List<String>): Line {
            return if (parts.size != 1) {
                InvalidLine("/leave command does not have arguments")
            } else LeaveRoomCommand()
        }

        private fun parseExit(parts: List<String>): Line {
            return if (parts.size != 1) {
                InvalidLine("/exit command does not have arguments")
            } else ExitCommand()
        }

        private fun parseShutdown(parts: List<String>): Line {
            return if (parts.size != 2) {
                InvalidLine("/shutdown must have a timeout param")
            } else ShutdownCommand(parts[1].toLong())
        }
    }

}