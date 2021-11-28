enum DEVS;
enum TEAMS;
array[DEVS] of bool: isSA;
array[TEAMS] of int: minSize;
array[TEAMS] of var set of DEVS: teams;
array[DEVS,TEAMS] of int: enjoy;
% Everyone must be part of a team
constraint array_union(teams) = DEVS;
include "all_disjoint.mzn";
% Team members can only be part of one team
constraint all_disjoint(teams);
% Minimum size of team
constraint forall(team in TEAMS)(card(teams[team]) >= minSize[team]);
% At least one SA per team
constraint forall(team in TEAMS)(sum(dev in teams[team])(isSA[dev]) >= 1);
% Total enjoyment = sum of enjoyments for realized team positions of each dev
constraint totalEnjoyment = sum(dev in DEVS, team in TEAMS where dev in teams[team])(enjoy[dev,team]);
var int: totalEnjoyment;
solve maximize totalEnjoyment;




