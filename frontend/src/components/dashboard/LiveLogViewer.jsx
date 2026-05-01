import React, { useState, useEffect, useRef } from 'react';
import { Terminal, Play, Pause, Search, Maximize2, Trash2, ShieldAlert, CheckCircle2, Info } from 'lucide-react';

const mockLogs = [
  { id: 1, level: 'INFO', time: '10:42:01', worker: 'worker-us-east-1a', msg: 'Initialized load test engine v2.0.4' },
  { id: 2, level: 'INFO', time: '10:42:02', worker: 'worker-us-east-1a', msg: 'Connecting to target http://api.internal.svc:8080' },
  { id: 3, level: 'INFO', time: '10:42:05', worker: 'worker-us-east-1b', msg: 'Spawning 500 virtual users...' },
  { id: 4, level: 'WARN', time: '10:42:15', worker: 'worker-us-east-1a', msg: 'Latency spike detected (>200ms) on /auth/login endpoint' },
  { id: 5, level: 'ERROR', time: '10:42:18', worker: 'worker-us-east-1b', msg: 'Connection timeout on DB pool acquirement. Retrying...' },
  { id: 6, level: 'INFO', time: '10:42:20', worker: 'worker-us-east-1a', msg: 'RPS stabilized at 1250 requests/sec' },
];

const getLevelColor = (level) => {
  switch(level) {
    case 'INFO': return 'text-blue-400';
    case 'WARN': return 'text-yellow-400';
    case 'ERROR': return 'text-red-400';
    default: return 'text-gray-400';
  }
};

const getLevelIcon = (level) => {
  switch(level) {
    case 'INFO': return <Info size={14} className="text-blue-400" />;
    case 'WARN': return <ShieldAlert size={14} className="text-yellow-400" />;
    case 'ERROR': return <ShieldAlert size={14} className="text-red-400" />;
    default: return <CheckCircle2 size={14} className="text-gray-400" />;
  }
};

const LiveLogViewer = () => {
  const [logs, setLogs] = useState(mockLogs);
  const [isPaused, setIsPaused] = useState(false);
  const [filter, setFilter] = useState('');
  const logEndRef = useRef(null);

  // Auto-scroll to bottom
  useEffect(() => {
    if (!isPaused) {
      logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [logs, isPaused]);

  // Simulate incoming logs
  useEffect(() => {
    if (isPaused) return;
    
    const interval = setInterval(() => {
      const newLog = {
        id: Date.now(),
        level: Math.random() > 0.8 ? 'WARN' : Math.random() > 0.9 ? 'ERROR' : 'INFO',
        time: new Date().toLocaleTimeString('en-US', { hour12: false }),
        worker: Math.random() > 0.5 ? 'worker-us-east-1a' : 'worker-us-east-1b',
        msg: `Executing VU sequence #${Math.floor(Math.random() * 10000)}... Status: OK [Latency: ${Math.floor(Math.random() * 150)}ms]`
      };
      setLogs(prev => [...prev.slice(-99), newLog]); // Keep last 100 logs
    }, 1500);

    return () => clearInterval(interval);
  }, [isPaused]);

  const filteredLogs = logs.filter(log => 
    log.msg.toLowerCase().includes(filter.toLowerCase()) || 
    log.worker.toLowerCase().includes(filter.toLowerCase()) ||
    log.level.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="card bg-aws-dark text-gray-300 font-mono border-gray-700 h-[400px] flex flex-col shadow-2xl relative overflow-hidden">
      {/* Terminal Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-gray-900 border-b border-gray-700 shrink-0 relative z-10">
        <div className="flex items-center gap-3">
          <Terminal size={18} className="text-aws-orange" />
          <h3 className="text-sm font-bold tracking-wider text-gray-100">Live Worker Console</h3>
          <span className="flex h-2 w-2 relative ml-2">
            {!isPaused && <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>}
            <span className={`relative inline-flex rounded-full h-2 w-2 ${isPaused ? 'bg-yellow-500' : 'bg-green-500'}`}></span>
          </span>
        </div>
        
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search size={14} className="absolute left-2 top-1/2 -translate-y-1/2 text-gray-500" />
            <input 
              type="text" 
              placeholder="Filter logs..." 
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="bg-gray-800 border border-gray-700 rounded text-xs px-2 pl-7 py-1 focus:outline-none focus:border-aws-orange transition-colors w-40 text-gray-200"
            />
          </div>
          <button onClick={() => setIsPaused(!isPaused)} className="p-1.5 hover:bg-gray-800 rounded text-gray-400 hover:text-white transition-colors" title={isPaused ? "Resume" : "Pause"}>
            {isPaused ? <Play size={16} /> : <Pause size={16} />}
          </button>
          <button onClick={() => setLogs([])} className="p-1.5 hover:bg-gray-800 rounded text-gray-400 hover:text-white transition-colors" title="Clear">
            <Trash2 size={16} />
          </button>
          <button className="p-1.5 hover:bg-gray-800 rounded text-gray-400 hover:text-white transition-colors">
            <Maximize2 size={16} />
          </button>
        </div>
      </div>

      {/* Terminal Body */}
      <div className="flex-1 overflow-y-auto p-4 space-y-1.5 text-[13px] relative z-10 scrollbar-thin scrollbar-thumb-gray-700 scrollbar-track-transparent">
        {filteredLogs.map((log) => (
          <div key={log.id} className="flex gap-3 hover:bg-gray-800/50 px-2 py-0.5 rounded group transition-colors">
            <span className="text-gray-500 shrink-0">[{log.time}]</span>
            <span className={`font-bold shrink-0 w-12 flex items-center gap-1 ${getLevelColor(log.level)}`}>
               {log.level}
            </span>
            <span className="text-purple-400 shrink-0">[{log.worker}]</span>
            <span className="text-gray-300 break-all">{log.msg}</span>
          </div>
        ))}
        {filteredLogs.length === 0 && (
          <div className="text-center text-gray-500 mt-10">No logs match the current filter.</div>
        )}
        <div ref={logEndRef} />
      </div>
      
      {/* Background Glitch/Scanline effect overlay */}
      <div className="absolute inset-0 pointer-events-none opacity-[0.03] bg-[linear-gradient(rgba(255,255,255,0)_50%,rgba(0,0,0,0.25)_50%),linear-gradient(90deg,rgba(255,0,0,0.06),rgba(0,255,0,0.02),rgba(0,0,255,0.06))] bg-[length:100%_4px,3px_100%] z-0"></div>
    </div>
  );
};

export default LiveLogViewer;
