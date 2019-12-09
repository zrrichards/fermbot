import json
from random import randrange

#just return some random data. This is just for testing
data1={
    'specificGravity': (1000.0+randrange(150))/1000,
    'currentTemp': randrange(30) + 32,
    'color': 'BLACK'
}

data2={
    'specificGravity': (1000.0+randrange(150))/1000,
    'currentTemp': randrange(30) + 32,
    'color': 'PINK'
}

def getData():
    return json.dumps([data1, data2])

if __name__ == "__main__": #dont run this as a module
    print getData()
