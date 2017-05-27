import logging
import sys
import termios
import time
import tty
import thread
import sqlite
import nfc
# Enable debug logging into log
DEBUG = True
# Enable printing informations to std. output
VERBOSE = True

class Actions:
    incomming = 1
    outcomming = 2
    breakstart = 3
    breakend = 4


if (DEBUG):
    logging.basicConfig(format='%(asctime)s %(message)s', filename='attendance.log', level=logging.DEBUG)

def debug(message):
    logging.debug(message)

def onScreen(message):
    if (VERBOSE):
        print(message)

def read():
    cardId = nfc.readNfc()
    return cardId

def readNfc(action):
    if (action == 55):  # 7 - Incomming
        # read the card id and insert data in the SQLite DB
        # other cases are optional
        cardId = read()
        logging.info("Incomming - %s", cardId)
        sqlite.insertReading(cardId, Actions.incomming)
    if (action == 57):  # 9 - outcomming
        cardId = read()
        logging.info("Outcomming - %s", cardId)
        sqlite.insertReading(cardId, Actions.outcomming)
    if (action == 49):  # 1 - break start
        cardId = read()
        logging.info("Break start - %s", cardId)
        sqlite.insertReading(cardId, Actions.breakstart)
    if (action == 51):  # 3 - break end
        cardId = read()
        logging.info("Break end - %s", cardId)
        sqlite.insertReading(cardId, Actions.breakend)
    if (action == 53):  # 5 - Deletion of last inserted action
        cardId = read()
        logging.info("Deleting last action - %s", cardId)
        (lastTime, lastAction) = sqlite.getLastReading(cardId) or (None, None)
        if (lastTime == None or lastAction == None):
            logging.info("Action not found")
            time.sleep(1)
    # Sleep a little, so the information about last action on display is readable by humans
    time.sleep(1)

# Backing up the input attributes, so we can change it for reading single
# character without hitting enter  each time
fd = sys.stdin.fileno()
old_settings = termios.tcgetattr(fd)

def getOneKey():
    try:
        tty.setcbreak(sys.stdin.fileno())
        ch = sys.stdin.read(1)
        return ord(ch)
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)

displayTime = True


def printDateToDisplay():
    while True:
        # Display current time on display, until global variable is set
        if displayTime != True:
            thread.exit()
        # display.lcdWriteFirstLine(time.strftime("%d.%m. %H:%M:%S", time.localtime()))
        # onScreen(time.strftime("%d.%m.%Y %H:%M:%S", time.localtime()))
        time.sleep(1)

def main():
    # 55 is a keyboard input for reading task
    readNfc(55)

if __name__ == '__main__':
    debug("----------========== Starting session! ==========----------")
    main()