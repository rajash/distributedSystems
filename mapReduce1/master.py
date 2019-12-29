import pika
import uuid
import json
from collections import Counter 
from wordcloud import WordCloud, STOPWORDS 
import matplotlib.pyplot as plt 
import pandas as pd

class MapRpcClient(object):
    
    def __init__(self):
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
        self.channel = self.connection.channel()
        result = self.channel.queue_declare(queue='', exclusive=True)
        self.callback_queue = result.method.queue
        self.channel.basic_consume(queue = self.callback_queue,
                                  on_message_callback = self.on_response,
                                  auto_ack=True)
        self.resp = {}
    
    def on_response(self, ch, method, props, body):
        if self.corr_id == props.correlation_id:
            self.response = body
    
    def call(self, func):
        self.response = None
        self.corr_id = str(uuid.uuid4())
        self.channel.basic_publish(
                                    exchange='',
                                    routing_key='rpc_queue',
                                    properties=pika.BasicProperties(
                                                                    reply_to=self.callback_queue,
                                                                    correlation_id = self.corr_id
                                                                    ,content_type='application/json'),
                                    body=json.dumps({'func':func}))
        print('sent' + json.dumps({'func':func}))
        while self.response is None:
            self.connection.process_data_events()
        self.resp = Counter(self.resp) + Counter(json.loads(self.response)) 
        self.resp = dict(self.resp)

func = """def map(text): 
            d = {} 

            # counting number of times each word comes up in list of words (in dictionary)
            for word in text.split(): 
                word = word.title()
                d[word] = d.get(word, 0) + 1
            return d
"""       
map_rpc = MapRpcClient()

for i in range(2):
    map_rpc.call(func)
    print(map_rpc.resp)



# with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
#     executor.map(map_rpc.call)

print(" [.] Workers Finshed.")

stopwords = set(STOPWORDS)
wordcloud = WordCloud(background_color="white",
                      width=1000,height=1000, 
                      colormap="Set1",
                      relative_scaling=0.5,
                      stopwords = stopwords,
                      collocations=False,
                      normalize_plurals=True).generate(''.join([(k+' ')*v for k,v in map_rpc.resp.items()]))

plt.figure(figsize = (20, 10), facecolor = None) 
plt.imshow(wordcloud) 
plt.axis("off") 
plt.tight_layout(pad = 0) 
  
plt.savefig('DS wordcloud.png') 
# print(dict(map_rpc.resp))
df = pd.DataFrame(list(map_rpc.resp.items()),columns=['Words', 'Frequency'])


df = df.loc[(~df['Words'].str.lower().isin(stopwords))]
            
df.sort_values('Frequency',ascending=False,inplace=True)

# print(stopwords)
plt.figure(figsize=(20,10))
plt.bar(df.iloc[:10].Words,df.iloc[:10].Frequency,color='#bc2024')
plt.grid(True)
plt.xticks(rotation=60)
plt.gca().set_frame_on(False)
for i,v in enumerate(df.iloc[:10].Frequency):
    plt.gca().text(i-0.3,v+0.5,str(v))

plt.savefig('Top 10 Most Frequent Words.png') 

df.to_csv('Words counts.csv')
