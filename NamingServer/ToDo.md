# 6DS_Project

## To Do:

### L3: NamingServer

- Create NamingServer (NS):
    - Create Interface + Implementation ✅ (Joeri)
- Create RESTController for NS ✅ (Elliot)
- Create MapSaver: save map on local machine and read the map ✅ (Thomas)

### L4: Discovery + Bootstrap (Thomas + Wout)

**General**

- Create Multicast (Wout)

**Entering node:**

- Calculate own hash and put it as currentID
- Send own name and IP-address to everyone using MC
- Receive message from naming server with #node in network:
    - #nodes = 1 -> nextID & prevID = currentID
    - #nodes > 1 -> will receive message from other nodes with 2 ID's

**namingServer:**

- Calculate the hash of the received node name
- Adds the hash and IP address in its map data structure (partially done in prev lab)
- Responds to the new node with the number of existing nodes that are currently in the network

**Other node:**

- Calculate hash entering node (enteringID)
- Check if entering node is new previous/next node (look slides)
    - Yes: answer (UC) to entering node with (startID, otherID) -> dependent of hash
    - No: do nothing

## L4: Shutdown (Elliot)

**Leaving node**

- Send nextID to prevNode
- Send prevID to nextNode
- Send leave message to NS

**Receiving node**

- Check if sender of message has nextID or prevID
- Replace that ID with the received ID in the message

**Naming server:**

- Remove node from map

## L4: Failure (Joeri)

- Create "Ping" method to check for connectivity

**Node receiving error**

- Ask for nextNode and prevNode of the failed node
- Update nextID and prevID of these nodes
- Ask to remove failed node from map in NS

**Naming server**

- Give nextNode and prevNode of failed node
- Remove failed node from map
