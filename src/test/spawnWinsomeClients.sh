#!/bin/bash
BWHT="\033[1;37m"
REG="(tput sgr0)"
LIB=../../lib/*:../
CLI=client.ClientMain

clients=(
    'java -cp '$LIB' '$CLI' "reward notification|login u1 pas1|show post 0|show feed|wallet btc|follow u3|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u2 pas2|post "TITOLO POST DI U2" "CHEMUSICA"|blog|wallet btc|follow u1|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u3 pas3|post "TITOLO POST DI U3" "BOO"|blog|wallet btc|follow mario"'
    'java -cp '$LIB' '$CLI' "reward notification|login u4 pas4|rate 2 1|follow u2|rate 2 -1|follow u4|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u5 pas5|rate 3 1|follow u3|comment 3 COMMENTONE INCREDIBILE|show feed|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u6 pas5|rate 3 1|follow u7|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u7 pas5|rate 3 1|follow u13|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u8 pas5|rate 3 1|follow u3|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u9 pas5|rate 3 1|follow u3|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u10 pas5|rate 3 1|follow u23|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u11 pas5|rate 3 1|follow u27|post "TITOLO POST DI U11" "BOO"|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u12 pas5|rate 3 1|follow u17|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u13 pas5|rate 3 1|follow u17|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u14 pas5|rate 3 1|follow u17|post "TITOLO POST DI U14" "BOO"|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u15 pas5|rate 3 1|follow u22|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u16 pas5|rate 3 1|follow u3|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u17 pas5|rate 3 1|follow u22|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u18 pas5|rate 3 1|follow u19|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u19 pas5|rate 3 1|follow u17|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u20 pas5|rate 3 1|follow u19|post "TITOLO POST DI U20" "BOO"|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u21 pas5|rate 3 1|follow u26|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u22 pas5|rate 3 1|follow u26|post "TITOLO POST DI U22" "BOO"|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u23 pas5|rate 3 1|follow u16|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u24 pas5|rate 3 1|follow u16|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u25 pas5|rate 3 1|follow u13|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u26 pas5|rate 3 1|follow u13|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u27 pas5|rate 3 1|follow u23|post "TITOLO POST DI U27" "BOO"|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u28 pas5|rate 3 1|follow u23|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u29 pas5|rate 3 1|follow u28|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u30 pas5|rate 3 1|follow u25|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    )

while true 
do
    i=$(( RANDOM % ${#clients[@]}))
    echo ${clients[i]} $BASHPID
    ${clients[i]}
done

# i=$(( RANDOM % ${#clients[@]}))
# echo ${clients[1]}
# ${clients[1]}
