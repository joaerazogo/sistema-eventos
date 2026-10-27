######################################
##        Variables Globales        ##
######################################

env  = "pdn"
tags = { project = "ticketing", env = "pdn" }

######################################
##          Variables SQS           ##
######################################

visibility_timeout = 30
message_retention  = 86400
max_receive_count  = 5
