package org.biergit

import com.google.ortools.sat.*
import mu.KotlinLogging
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger {}

/**
 * Represents a developer which wants to work in a nice team to be productive
 * @property sa is this dev a highly experienced Software Brownfieldhitect?
 */
data class Developer(val name: String, val sa: Boolean = false) {
    override fun toString(): String = name
}

class ATeamsSolver {

    private fun createNormalDevs(vararg names: String): Array<Developer> {
        return names.map { Developer(it) }.toTypedArray()
    }

    private fun createSADevs(vararg names: String): Array<Developer> {
        return names.map { Developer(it, true) }.toTypedArray()
    }

    fun solve() {
        val developers = createNormalDevs(
                "Christian"
                , "Andreas"
                , "Ann"
                , "Camilla"
                , "Michelle"
                , "Rebecca"
                , "Roger"
                , "Hans-Wurst"
                , "Jessica"
        ) + createSADevs(
                "Matt"
                , "Frank"
                , "Susan"
        )

        val devMap = developers.associateBy { it.name }


        var sympathies = Array(developers.size) { Array(developers.size) { 0 } }
        with(sympathies) {
            fun setSympathy(sympather: String, sympathee: String, sympathyValue: Int) {
                get(developers.indexOf(devMap[sympather]))[developers.indexOf(devMap[sympathee])] = sympathyValue;
            }

            setSympathy("Matt", "Christian", 8)
            setSympathy("Christian", "Andreas", -4)
            setSympathy("Rebecca", "Frank", 5)
            setSympathy("Rebecca", "Matt", 4)
            setSympathy("Michelle", "Andreas", 7)
            setSympathy("Roger", "Rebecca", 2)
            setSympathy("Michelle", "Roger", 3)
            setSympathy("Michelle", "Rebecca", 2)
            setSympathy("Hans-Wurst", "Andreas", -10)
            setSympathy("Susan", "Jessica", 8)
            setSympathy("Susan", "Andreas", -8)
            setSympathy("Jessica", "Andreas", 5)
            setSympathy("Christian", "Susan", 6)
            setSympathy("Camilla", "Susan", 3)
            setSympathy("Camilla", "Rebecca", -7)
            setSympathy("Camilla", "Hans-Wurst", 2)

        }

        val condensedSympathies = sympathies.mapIndexed { index, sympathyValues -> sympathyValues.mapIndexed { idx, sympathyValue -> sympathyValue + sympathies[idx][index] } }.mapIndexed { index, list -> list.filterIndexed { idx, _ -> idx > index } }
        logger.info { "Condensed sympathies: $condensedSympathies" }

        val teamNames = arrayOf("Greenfield", "Brownfield", "Darkside")
        var teamPreferences = Array(teamNames.size) { Array(developers.size) { 0 } }
        with(teamPreferences) {
            fun setPreference(dev: String, team: String, preferenceValue: Int) {
                get(teamNames.indexOf(team))[developers.indexOf(devMap[dev])] = preferenceValue;
            }
            setPreference("Matt", "Brownfield", 7)
            setPreference("Christian", "Darkside", 5)
            setPreference("Christian", "Greenfield", 6)
            setPreference("Frank", "Brownfield", 8)
            setPreference("Susan", "Darkside", 8)
            setPreference("Rebecca", "Greenfield", -10)
            setPreference("Camilla", "Darkside", -4)
        }
        val minTeamSize = developers.size / teamNames.size
        val minSAPerTeam = 1;

        var allTeamAssignments = arrayOf<Array<IntVar>>()

        var model = CpModel()
        // Assign Boolean vars to all teams to mark employees in the team
        for (team in teamNames.indices) {
            var aTeamAssignment = arrayOf<IntVar>()
            for (dev in developers.indices) {
                aTeamAssignment += model.newBoolVar(developers[dev].toString() + " in team " + teamNames[team])
            }
            allTeamAssignments += aTeamAssignment
        }


        // Setup sympathy constraints
        var sympathiesInUse = emptyArray<IntVar>()

        for (i in condensedSympathies.indices) {
            for (j in condensedSympathies[i].indices) {
                val sympathy = condensedSympathies[i][j].toLong()
                if (sympathy != 0L) {
                    logger.info { "Sympathy between " + developers[i] + " and " + developers[j + i + 1] + ": " + sympathy }
                    for (k in teamNames.indices) {
                        var sympathyVar = model.newIntVar(-10L, 10L, "Sympathy between " + developers[i] + " and " + developers[j + i + 1] + " in team " + teamNames[k])
                        sympathiesInUse += sympathyVar
                        // set to sympathy when both in same team
                        model.addEquality(sympathyVar, sympathy).onlyEnforceIf(arrayOf(allTeamAssignments[k][i], allTeamAssignments[k][j + i + 1]))
                        // else set to 0
                        model.addEquality(sympathyVar, 0L).onlyEnforceIf(allTeamAssignments[k][i].not())
                        model.addEquality(sympathyVar, 0L).onlyEnforceIf(allTeamAssignments[k][j + i + 1].not())
                    }

                }
            }
        }


//        //Setup team preference constraints
        var teamPreferencesInUse = emptyArray<IntVar>()

        for (i in teamPreferences.indices) {
            for (j in teamPreferences[i].indices) {
                val teamPreference = teamPreferences[i][j].toLong()
                if (teamPreference != 0L) {
                    logger.info { "Team preference of " + developers[j] + " for " + teamNames[i] + ": " + teamPreference }
                    var teamPreferenceVar = model.newIntVar(-10L, 10L, "Preference of " + developers[j] + " for " + teamNames[i])
                    teamPreferencesInUse += teamPreferenceVar
                    model.addEquality(teamPreferenceVar, teamPreference).onlyEnforceIf(allTeamAssignments[i][j])
                    // else set to 0
                    model.addEquality(teamPreferenceVar, 0L).onlyEnforceIf(allTeamAssignments[i][j].not())

                }
            }
        }

        //Maximize sympathy :) and preferred team choice
//        model.maximize(LinearExpr.sum(sympathiesInUse))
        model.maximize(LinearExpr.sum(sympathiesInUse + teamPreferencesInUse))

        // Add the constraints
        // One employee in one team only
        for (dev in developers.indices) {
            model.addEquality(LinearExpr.sum(allTeamAssignments.map { it[dev] }.toTypedArray()), 1L)
        }

        // Teams of minTeamSize minimum size
        for (team in teamNames.indices) {
            model.addGreaterOrEqual(LinearExpr.sum(allTeamAssignments[team]), minTeamSize.toLong())
        }

        // At least minSAPerTeam SAs per team
        for (team in teamNames.indices) {
            model.addGreaterOrEqual(LinearExpr.sum(allTeamAssignments[team].filterIndexed { index, _ -> developers.withIndex().filter { it.value.sa }.map { it.index }.contains(index) }.toTypedArray()), minSAPerTeam.toLong())
        }

        var solver = CpSolver()

        var result = solver.searchAllSolutions(model, object : CpSolverSolutionCallback() {
            private var numberOfSolutions by Delegates.notNull<Int>()
            private var teamNames by Delegates.notNull<Array<String>>()
            private var devs by Delegates.notNull<Array<Developer>>()
            private var allTeamAssignments by Delegates.notNull<Array<Array<IntVar>>>()
            private var sympathiesInUse by Delegates.notNull<Array<IntVar>>()
            private var teamPreferencesInUse by Delegates.notNull<Array<IntVar>>()

            private var solutionCounter = 0;

            override fun onSolutionCallback() {
                if (numberOfSolutions >= solutionCounter) {
                    logger.info("Solution $solutionCounter")
                    solutionCounter = solutionCounter.inc()
                    printSolution()
                    println()
                } else {
                    logger.info { "Stopping search" }
                    stopSearch()
                }
            }

            private fun printSolution() {
                for (team in teamNames.indices) {
                    logger.info { "==========================================================" }
                    var devsInTeam = arrayOf<Developer>()
                    for (dev in developers.indices) {
                        if (booleanValue(allTeamAssignments[team][dev])) {
                            devsInTeam += developers[dev]
                        }
                    }
                    logger.info { "Team " + teamNames[team] + ": " + devsInTeam.joinToString() }
                }
                logger.info { "==========================================================" }
                logger.info { "Global sympathy: " + objectiveValue() }
                logger.info { sympathiesInUse.filter { value(it) != 0L }.joinToString("\n", prefix="Sympathies:\n") { it.name + " : " + value(it) } }
                logger.info { teamPreferencesInUse.filter { value(it) != 0L }.joinToString("\n", prefix = "Team preferences:\n") { it.name + " : " + value(it) } }
                logger.info { "Number of branches: " + numBranches() }
                logger.info { "Number of conflicts: " + numConflicts() }
            }


            fun init(teamNames: Array<String>, devs: Array<Developer>, allTeamAssignments: Array<Array<IntVar>>, sympathiesInUse: Array<IntVar>, teamPreferencesInUse: Array<IntVar>, numberOfSolutions: Int): CpSolverSolutionCallback {
                this.teamNames = teamNames
                this.devs = devs
                this.allTeamAssignments = allTeamAssignments
                this.sympathiesInUse = sympathiesInUse
                this.teamPreferencesInUse = teamPreferencesInUse
                this.numberOfSolutions = numberOfSolutions
                return this
            }

        }.init(teamNames, developers, allTeamAssignments, sympathiesInUse, teamPreferencesInUse, 100))


        logger.info { result }
        logger.info { "Duration " + solver.wallTime() }

    }

    companion object {
        init {
            System.loadLibrary("jniortools");
        }
    }
}
