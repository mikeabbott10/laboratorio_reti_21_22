#!/bin/bash
BWHT="\033[1;37m"
REG=$(tput sgr0)
TIMER=120
LIB=../../lib/*:../
export BWHT

#run server in background
#java -cp $LIB server.ServerMain &
#export S_PID=$!
#Server PID = $S_PID

echo -e $BWHT "

    STARTING STRESS TEST
    20 Clients will run simultaneously for ${TIMER}s

" $REG

sleep 2

start=$SECONDS

for i in {1..20}
do
    ./spawnWinsomeClients.sh &
done

echo -e $BWHT "
    SLEEPING
" $REG

sleep ${TIMER}

echo -e $BWHT "
    KILLING CLIENTS
" $REG

killall -9 spawnWinsomeClients.sh | at now &> /dev/null

duration=$(( SECONDS - start ))

sleep 2

echo -e $BWHT "

    Well done! (${duration}s)

" $REG
