import sys

import bluetooth._bluetooth as bluez

import blescan
import json

#  Fermbot - Open source fermentation monitoring software.
#  Copyright (C) 2019 Zachary Richards
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.

    #Adapted from https://github.com/atlefren/pytilt/blob/master/pytilt.py


TILTS = {
        'a495bb10c5b14b44b5121370f02d74de': 'RED',
        'a495bb20c5b14b44b5121370f02d74de': 'GREEN',
        'a495bb30c5b14b44b5121370f02d74de': 'BLACK',
        'a495bb40c5b14b44b5121370f02d74de': 'PURPLE',
        'a495bb50c5b14b44b5121370f02d74de': 'ORANGE',
        'a495bb60c5b14b44b5121370f02d74de': 'BLUE',
        'a495bb70c5b14b44b5121370f02d74de': 'YELLOW',
        'a495bb80c5b14b44b5121370f02d74de': 'PINK',
}


def distinct(objects):
    seen = set()
    unique = []
    for obj in objects:
        if obj['uuid'] not in seen:
            unique.append(obj)
            seen.add(obj['uuid'])
    return unique



def monitor_tilt(sock):
    while True:
        beacons = distinct(blescan.parse_events(sock, 10))
        for beacon in beacons:
            if beacon['uuid'] in TILTS.keys():
                data={
                    'color': TILTS[beacon['uuid']],
                    'currentTemp': beacon['major'],
                    'specificGravity': beacon['minor'] / 1000.0
                }
                return json.dumps(data)


def get_data():
    dev_id = 0
    try:
        sock = bluez.hci_open_dev(dev_id)
    except:
        print 'error accessing bluetooth device...'
        sys.exit(1)

    blescan.hci_le_set_scan_parameters(sock)
    blescan.hci_enable_le_scan(sock)
    return monitor_tilt(sock)

if __name__ == '__main__':
    print get_data()
