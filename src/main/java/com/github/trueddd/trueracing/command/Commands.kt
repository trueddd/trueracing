package com.github.trueddd.trueracing.command

sealed class Commands {

    companion object {

        private val list = listOf(
            Track.List,
            Track.Lights,
            Track.Create,
            Track.Delete,
            Track.Finish.Create,
            Track.Finish.Delete,
            Race.Register,
            Race.Start,
            Race.Stop,
            Team.List,
            Team.Create,
            Team.Delete,
            Test,
        )

        fun parse(name: String, args: List<String>): Pair<Commands, List<String>>? {
            val full = "$name ${args.joinToString(" ")}".trim()
            for (command in list) {
                command.regex.matchEntire(full)?.let {
                    return command to it.groupValues.drop(1)
                } ?: continue
            }
            return null
        }
    }

    abstract val regex: Regex

    object Test : Commands() {
        override val regex = Regex("^test$")
    }

    sealed class Team : Commands() {

        object List : Team() {
            override val regex = Regex("^team-list$")
        }

        object Create : Team() {
            override val regex = Regex("^team-create ([\\w\\d-_]+) ([a-f\\d])$")
        }

        object Delete : Team() {
            override val regex = Regex("^team-delete ([\\w\\d-_]+)$")
        }
    }

    sealed class Track : Commands() {

        object List : Track() {
            override val regex = Regex("^track list$")
        }

        object Lights : Track() {
            override val regex = Regex("^track-lights ([\\w\\d-_]+)$")
        }

        object Create : Track() {
            override val regex = Regex("^track-create ([\\w\\d-_]+) ([\\d]+)$")
        }

        object Delete : Track() {
            override val regex = Regex("^track delete ([\\w\\d-_]+)$")
        }

        sealed class Finish : Track() {

            object Create : Finish() {
                override val regex = Regex("^track finish create ([\\w\\d-_]+)$")
            }

            object Delete : Finish() {
                override val regex = Regex("^track finish delete ([\\w\\d-_]+)$")
            }
        }
    }

    sealed class Race : Commands() {

        object Start : Commands() {
            override val regex = Regex("^race-start ([\\w\\d-_]+)$")
        }

        object Stop : Commands() {
            override val regex = Regex("^race-stop ([\\w\\d-_]+)$")
        }

        object Register : Commands() {
            override val regex = Regex("^race-register ([\\w\\d-_]+)(?: ([\\w\\d_]{3,16}))+$")
        }

//        object Delete : Commands() {
//            override val regex = Regex("^race-delete ")
//        }
    }
}
