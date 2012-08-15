#!/usr/bin/python
import csv
from collections import namedtuple

Ticket = namedtuple('Ticket', ["ID","NAME","EVENT_DATE","RESERVATION_NR","NOTE","CONTACT","NUM_FOOD_TICKETS","NUM_FOOD_TICKETS_REDUCED","NUM_SHOW_TICKETS","NUM_SHOW_TICKETS_REDUCED","AMOUNT_PAID","PACKAGED","DELETED"])
tickets = map(Ticket._make, csv.reader(open("blt-bak-20110703_195547.csv", "rb")))

dates = ["28.07.2011", "30.07.2011", "31.07.2011"]
print [ (tick.NUM_SHOW_TICKETS, tick.NUM_SHOW_TICKETS_REDUCED ) for tick in tickets if tick.EVENT_DATE == '28.07.2011' ]
for date in dates:
  sum_p = reduce(lambda x, y: x + int(y.NUM_SHOW_TICKETS) + int(y.NUM_SHOW_TICKETS_REDUCED), [ tick for tick in tickets if tick.EVENT_DATE == date ], 0)
  sum_g = reduce(lambda x, y: x + int(y.NUM_FOOD_TICKETS) + int(y.NUM_FOOD_TICKETS_REDUCED), [ tick for tick in tickets if tick.EVENT_DATE == date ], 0)
  print "%s: %d G, %d P" %(date, sum_g, sum_p)