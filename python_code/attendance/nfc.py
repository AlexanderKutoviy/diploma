import MFRC522


# read NFC card and return String with ID
def readNfc():
    reading = True
    while reading:
        MIFAREReader = MFRC522.MFRC522()

        (status, TagType) = MIFAREReader.MFRC522_Request(MIFAREReader.PICC_REQIDL)

        (status, backData) = MIFAREReader.MFRC522_Anticoll()
        if status == MIFAREReader.MI_OK:
            MIFAREReader.AntennaOff()
            reading = False
            return str(backData[0]) + str(backData[1]) + str(backData[2]) + str(backData[3]) + str(backData[4])
