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
    'java -cp '$LIB' '$CLI' "reward notification|login u5 pas5|rate 3 1|follow u3|comment 3 COMMENTONE INCREDIBILE|show feed|unfollow u3|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u6 pas6|rate 3 1|follow u7|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u7 pas7|rate 3 1|follow u13|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u8 pas8|rate 3 1|follow u3|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u9 pas9|rate 3 1|follow u3|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u10 pas10|rate 3 1|follow u14|follow u23|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u11 pas11|rate 3 1|follow u14|follow u27|post "TITOLO POST DI U11" "BOO"|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u12 pas12|rate 3 1|follow u14|follow u17|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u13 pas13|rate 3 1|follow u14|follow u17|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u14 pas14|rate 3 1|follow u14|follow u17|post "TITOLO POST DI U14" "BOO"|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u15 pas15|rate 3 1|follow u14|follow u22|comment 70 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u16 pas16|rate 3 1|follow u14|follow u3|comment 3 COMMENT OLE CIAO del 16|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u17 pas17|rate 3 1|follow u14|follow u22|comment 70 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u18 pas18|rate 3 1|follow u14|follow u19|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u19 pas19|rate 3 1|follow u14|follow u17|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u20 pas20|rate 3 1|follow u14|follow u19|post "TITOLO POST DI U20" "BOO"|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u21 pas21|rate 3 1|follow u14|follow u26|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u22 pas22|rate 3 1|follow u14|follow u26|post "TITOLO POST DI U22" "BOO"|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u23 pas23|rate 3 1|follow u14|follow u16|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u24 pas24|rate 3 1|follow u14|follow u16|comment 3 COMMENTONE INCREDIBILE|show feed|wallet|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u25 pas25|rate 3 1|follow u14|follow u13|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u26 pas26|rate 3 1|follow u14|follow u13|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u27 pas27|rate 3 1|follow u14|follow u23|post "TITOLO POST DI U27" "BOO"|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u28 pas28|rate 3 1|follow u14|follow u23|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u29 pas29|rate 3 1|follow u14|follow u28|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    'java -cp '$LIB' '$CLI' "reward notification|login u30 pas30|rate 3 1|follow u14|follow u25|comment 3 COMMENTONE INCREDIBILE|show feed|wallet btc|logout"'
    )

while true 
do
    i=$(( RANDOM % ${#clients[@]}))
    echo ${clients[i]} $BASHPID
    ${clients[i]}
done