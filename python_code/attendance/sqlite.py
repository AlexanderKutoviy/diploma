import sqlite3
from time import strftime,localtime
import datetime

# TODO: change this code for SQLite needs

def connect():
    # Mysql connection setup. Insert your values here
    return sqlite3.connect('attendance.db')

def insertReading(tagId,action):
    conn = connect()
    cur = conn.cursor()
    currentTime=strftime("%Y%m%d%H%M%S", localtime())
    print(tagId)
    print(currentTime)
    cur.execute("INSERT INTO readings (tagId, time, action) VALUES (?, ?, ?)",(tagId,currentTime,action))
    cur.close()
    conn.commit()
    conn.close()
    print("INSERTED")


def getLastReading(tagId):
    # '''Returns last reading inserted max 5 minutes ago in array (time,action)'''
    checkTime = datetime.datetime.now() - datetime.timedelta(minutes=5)
    db = connect()
    cur = db.cursor()
    cur.execute("SELECT time, action FROM readings WHERE tagId=? AND time>? ORDER BY time DESC LIMIT 1",(tagId,checkTime.strftime("%Y%m%d%H%M%S")))
    row = cur.fetchone()
    db.close()
    return row

def deleteLastReading(tagId):
    checkTime = datetime.datetime.now() - datetime.timedelta(minutes=6)
    db = connect()
    cur = db.cursor()
    cur.execute("DELETE FROM readings WHERE tagId=? AND time>? ORDER BY time DESC LIMIT 1",(tagId,checkTime.strftime("%Y%m%d%H%M%S")))
    row = cur.fetchone()
    db.close()