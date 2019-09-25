import java.util.*;

class Main {
  public static void main(String[] args) throws InterruptedException {
    TaskQueue queue = new TaskQueue();
    
    queue.push(new MyTask(1,25));
    queue.push(new MyTask(26,100));
    queue.push(new MyTask(101,320));
    queue.push(new MyTask(321,499));
    queue.push(new MyTask(500,780));
    queue.push(new MyTask(781,1100));
    queue.push(new MyTask(1101,2530));
    queue.push(new MyTask(2531,3210));
    
    WorkerThread t1 = new WorkerThread(queue, 0);
    WorkerThread t2 = new WorkerThread(queue, 1);
    WorkerThread t3 = new WorkerThread(queue, 1);
    
    t2.start();
    t3.start();
    t1.start();
    
    
    t1.join();
    t2.join();
    t3.join();
    
  }
}

class WorkerThread extends Thread {
    TaskQueue queue;
    int type;
    
    public WorkerThread(TaskQueue q, int t){
      this.queue = q;
      this.type = t;
    }

    @Override
    public void run() {
      if (type == 0) // Thread reading queue
      {
        MyTask t = queue.pop();
        while(t != null){
          System.out.println(t.execute());
          t = queue.pop();
        }
      }else{ // Thread pushing new tasks
        queue.push(new MyTask(5000,7000));
        queue.push(new MyTask(8000,11000));
        queue.push(new MyTask(11001,20000));
      }
      
    }
}


class TaskQueue {
  
  private LinkedList<MyTask> tasks = new LinkedList<MyTask>();
  
  public synchronized MyTask pop(){
    synchronized(tasks){
      try {
        return tasks.removeFirst();
      }catch(NoSuchElementException e){
        return null;
      }
    }
  }
  
  public void push(MyTask t){
    synchronized(tasks){
      tasks.addLast(t);
    }
  }
  
}

class MyTask {
  int start;
  int end;
  
  public MyTask(int s, int e){
    this.start = s;
    this.end = e;
  }
  
  public ArrayList<Integer> execute(){
    ArrayList<Integer> primes = new ArrayList<Integer>();
      int prime = 2;
      
      while( prime <= this.end){
        boolean isPrime = true;
        for (int j=2; j<prime; j++){
          if (prime%j ==0){
            isPrime = false;
            break;
          }
        }
        
        if (isPrime){
          if(prime >= this.start)
            primes.add(prime);
        } 
        prime++;
       }
      return primes;
  }
  
  
}