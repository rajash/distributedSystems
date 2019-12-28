import pika
import uuid
import json
from collections import Counter 
from wordcloud import WordCloud, STOPWORDS 
import matplotlib.pyplot as plt 

class MapRpcClient(object):
    
    def __init__(self):
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
        self.channel = self.connection.channel()
        result = self.channel.queue_declare(queue='', exclusive=True)
        self.callback_queue = result.method.queue
        self.channel.basic_consume(queue = self.callback_queue,
                                  on_message_callback = self.on_response,
                                  auto_ack=True)
    
    def on_response(self, ch, method, props, body):
        if self.corr_id == props.correlation_id:
            self.response = body
    
    def call(self, func,s):
        self.response = None
        self.corr_id = str(uuid.uuid4())
        self.channel.basic_publish(
                                    exchange='',
                                    routing_key='rpc_queue',
                                    properties=pika.BasicProperties(
                                                                    reply_to=self.callback_queue,
                                                                    correlation_id = self.corr_id
                                                                    ,content_type='application/json'),
                                    body=json.dumps({'seek':s,
                                                    'func':func}))
        print('sent' + json.dumps({'seek':s,
                                                    'func':func}))
        while self.response is None:
            self.connection.process_data_events()
        return json.loads(self.response)        

func = """def map(text): 
            d = {} 

            # counting number of times each word comes up in list of words (in dictionary)
            for word in text.split(): 
                d[word] = d.get(word, 0) + 1
            return d
"""       
map_rpc = MapRpcClient()
response = {}
s = 0
r = map_rpc.call(func, s)
while len(r):
    response = Counter(response) + Counter(r) 
    s += 5120
    r = map_rpc.call(func, s)

print(" [.] Workers Finshed.")

stopwords = set(STOPWORDS)
wordcloud = WordCloud(background_color="white",
                      width=1000,height=1000, 
                      colormap="Set1",
                      relative_scaling=0.5,
                      stopwords = stopwords,
                      collocations=False,
                      normalize_plurals=True).generate(''.join([(k+' ')*v for k,v in response.items()]))

plt.figure(figsize = (20, 10), facecolor = None) 
plt.imshow(wordcloud) 
plt.axis("off") 
plt.tight_layout(pad = 0) 
  
plt.savefig('DS wordcloud.png') 