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
