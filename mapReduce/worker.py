import pika
import json

connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
channel = connection.channel()
channel.queue_declare(queue='rpc_queue')

def on_request(ch, method, props, body):
    request = json.loads(body)
    print(' [*] Got ')
    print(request)    
    f = open("mapper.py", "w")
    f.write(request['func'])
    f.close()
    
    from mapper import map    
    f = open('distributed systems.txt', 'r', encoding='utf-8')
    f.seek(int(request['seek']))
    if int(request['seek']) != 0:
        f.readline()
    text = ''.join(f.readlines(5120))
    
    response = map(text)
    
    ch.basic_publish(exchange='',
                    routing_key=props.reply_to,
                    properties=pika.BasicProperties(correlation_id = props.correlation_id,content_type='application/json'),
                    body = json.dumps(response))
    ch.basic_ack(delivery_tag = method.delivery_tag)

channel.basic_qos(prefetch_count=1)
channel.basic_consume(queue='rpc_queue', on_message_callback = on_request)
print(" [x] Awaiting RPC requests")
channel.start_consuming()