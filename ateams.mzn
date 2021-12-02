enum DEVS;
enum TEAMS;
% is the dev a software architect?
array[DEVS] of bool: isSA;
array[TEAMS] of int: minSize;
array[TEAMS] of var set of DEVS: teams;
array[DEVS,TEAMS] of int: enjoy;
array[DEVS,DEVS] of int: sympathy;
% each dev can only assign <totalPoints> to claim enjoyment for a team or sympathy for a colleague. No negative points allowed.
int: totalPoints = 10;
constraint forall(dev in DEVS)(assert(sum(team in TEAMS)(enjoy[dev,team]) + sum(otherDev in DEVS where otherDev != dev)(sympathy[dev,otherDev])<= totalPoints, "Points for sympathy and enjoyment are restricted to \(totalPoints) in total."));
constraint forall(dev in DEVS,otherDev in DEVS where otherDev != dev)(assert(sympathy[dev,otherDev] >= 0, "Negative sympathy not allowed."));
constraint forall(dev in DEVS,team in TEAMS)(assert(enjoy[dev,team] >= 0, "Negative enjoyment not allowed."));
% Everyone must be part of a team
constraint array_union(teams) = DEVS;
include "all_disjoint.mzn";
% A dev can only be part of one team
constraint all_disjoint(teams);
% Minimum size of team
constraint forall(team in TEAMS)(card(teams[team]) >= minSize[team]);
% At least one SA per team
constraint forall(team in TEAMS)(sum(dev in teams[team])(isSA[dev]) >= 1);
% Total enjoyment = sum of enjoyments for realized team position of each dev
constraint totalEnjoyment = sum(dev in DEVS, team in TEAMS where dev in teams[team])(enjoy[dev,team]);
var int: totalEnjoyment;
var int: totalSympathy;
% Total sympathy = sum of sympathies for devs being in one team
constraint totalSympathy = sum(devOne in DEVS, devTwo in DEVS, team in TEAMS where devOne < devTwo /\ devOne in teams[team] /\ devTwo in teams[team])(sympathy[devOne,devTwo] + sympathy[devTwo,devOne]);
var int: rating;
constraint rating = totalSympathy + totalEnjoyment;
solve maximize rating;




