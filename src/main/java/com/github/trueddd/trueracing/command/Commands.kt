package com.github.trueddd.trueracing.command

sealed class Commands {

    companion object {

        private val list = listOf(
            Track.List,
            Track.Create,
            Track.Delete,
            Race,
        )

        fun parse(name: String, args: List<String>): Pair<Commands, List<String>>? {
            val full = "$name ${args.joinToString(" ")}"
            for (command in list) {
                command.regex.matchEntire(full)?.let {
                    return command to it.groupValues.drop(1)
                } ?: continue
            }
            return null
        }
    }

    abstract val tag: String
    abstract val regex: Regex

    sealed class Track : Commands() {

        object List : Track() {
            override val tag = "track list"
            override val regex = Regex("^track list$")
        }

        object Create : Track() {
            override val tag = "track create"
            override val regex = Regex("^track create ([\\w\\d-_]+)$")
        }

        object Delete : Track() {
            override val tag = "track delete"
            override val regex = Regex("^track delete ([\\w\\d-_]+)$")
        }

        sealed class Finish : Track() {

            object Create : Finish() {
                override val tag = "track finish create"
                override val regex = Regex("^track finish create ([\\w\\d-_]+)$")
            }

            object Delete : Finish() {
                override val tag = "track finish delete"
                override val regex = Regex("^track finish delete ([\\w\\d-_]+)$")
            }
        }
    }

    object Race : Commands() {

        override val tag = "race"
        override val regex = Regex("^race$")
    }
}
