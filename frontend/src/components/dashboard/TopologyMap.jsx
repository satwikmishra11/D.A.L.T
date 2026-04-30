import React, { useEffect, useRef } from 'react';
import { Activity, Server, Database, Cloud } from 'lucide-react';

const nodes = [
  { id: 'gateway', label: 'API Gateway', x: 50, y: 50, type: 'cloud', status: 'healthy' },
  { id: 'auth', label: 'Auth Service', x: 20, y: 150, type: 'server', status: 'healthy' },
  { id: 'users', label: 'User Profile', x: 80, y: 150, type: 'server', status: 'warning' },
  { id: 'db', label: 'Primary DB', x: 50, y: 250, type: 'db', status: 'healthy' },
];

const edges = [
  { source: 'gateway', target: 'auth', load: 'low' },
  { source: 'gateway', target: 'users', load: 'high' },
  { source: 'auth', target: 'db', load: 'medium' },
  { source: 'users', target: 'db', load: 'high' },
];

const TopologyMap = () => {
  const canvasRef = useRef(null);

  // Animation logic for drawing traffic particles along edges
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let animationFrameId;
    let particles = [];

    // Initialize particles
    edges.forEach(edge => {
      const sourceNode = nodes.find(n => n.id === edge.source);
      const targetNode = nodes.find(n => n.id === edge.target);
      
      const particleCount = edge.load === 'high' ? 5 : edge.load === 'medium' ? 3 : 1;
      const speed = edge.load === 'high' ? 2 : edge.load === 'medium' ? 1.5 : 1;
      
      for (let i = 0; i < particleCount; i++) {
        particles.push({
          source: sourceNode,
          target: targetNode,
          progress: Math.random(), // 0 to 1
          speed: speed * 0.01,
          color: edge.load === 'high' ? '#ef4444' : edge.load === 'medium' ? '#f59e0b' : '#10b981'
        });
      }
    });

    const draw = () => {
      const width = canvas.width;
      const height = canvas.height;
      
      // Clear canvas
      ctx.clearRect(0, 0, width, height);
      
      // Draw static edges
      edges.forEach(edge => {
        const sourceNode = nodes.find(n => n.id === edge.source);
        const targetNode = nodes.find(n => n.id === edge.target);
        
        const sx = (sourceNode.x / 100) * width;
        const sy = (sourceNode.y / 300) * height; // normalized against a 300 height coordinate system
        const tx = (targetNode.x / 100) * width;
        const ty = (targetNode.y / 300) * height;

        ctx.beginPath();
        ctx.moveTo(sx, sy);
        ctx.lineTo(tx, ty);
        ctx.strokeStyle = '#e5e7eb'; // gray-200
        ctx.lineWidth = 2;
        ctx.stroke();
      });

      // Update and draw particles
      particles.forEach(p => {
        p.progress += p.speed;
        if (p.progress >= 1) p.progress = 0;

        const sx = (p.source.x / 100) * width;
        const sy = (p.source.y / 300) * height;
        const tx = (p.target.x / 100) * width;
        const ty = (p.target.y / 300) * height;

        const currentX = sx + (tx - sx) * p.progress;
        const currentY = sy + (ty - sy) * p.progress;

        ctx.beginPath();
        ctx.arc(currentX, currentY, 3, 0, Math.PI * 2);
        ctx.fillStyle = p.color;
        ctx.fill();
        // Add glow
        ctx.shadowBlur = 10;
        ctx.shadowColor = p.color;
      });

      animationFrameId = requestAnimationFrame(draw);
    };

    draw();
    return () => cancelAnimationFrame(animationFrameId);
  }, []);

  return (
    <div className="card lg:col-span-2 relative h-[350px] overflow-hidden flex flex-col">
      <div className="flex justify-between items-center mb-4 z-10">
        <div>
          <h3 className="font-bold text-xl text-gray-900">Topology Map</h3>
          <p className="text-sm text-gray-500">Live service architecture & traffic</p>
        </div>
        <div className="flex gap-2">
            <span className="flex items-center gap-1 text-xs font-medium text-gray-500"><div className="w-2 h-2 rounded-full bg-green-500"></div> Healthy</span>
            <span className="flex items-center gap-1 text-xs font-medium text-gray-500"><div className="w-2 h-2 rounded-full bg-yellow-500"></div> Warning</span>
        </div>
      </div>
      
      <div className="absolute inset-0 top-16 bg-gray-50/50 rounded-xl">
        <canvas 
          ref={canvasRef} 
          width={800} 
          height={300} 
          className="w-full h-full"
          style={{ display: 'block', width: '100%', height: '100%' }}
        />
        
        {/* Render HTML Nodes over Canvas */}
        {nodes.map(node => {
          const Icon = node.type === 'cloud' ? Cloud : node.type === 'server' ? Server : Database;
          const statusColor = node.status === 'healthy' ? 'border-green-400 text-green-600 bg-green-50' : 'border-yellow-400 text-yellow-600 bg-yellow-50 animate-pulse-slow';
          
          return (
            <div 
              key={node.id}
              className={`absolute transform -translate-x-1/2 -translate-y-1/2 flex flex-col items-center gap-1 cursor-pointer hover:scale-110 transition-transform`}
              style={{ left: `${node.x}%`, top: `${(node.y / 300) * 100}%` }}
            >
              <div className={`p-3 rounded-xl border-2 shadow-sm bg-white ${statusColor}`}>
                <Icon size={20} strokeWidth={2.5} />
              </div>
              <span className="text-xs font-bold text-gray-700 bg-white/80 backdrop-blur px-2 py-0.5 rounded-full shadow-sm">{node.label}</span>
            </div>
          )
        })}
      </div>
    </div>
  );
};

export default TopologyMap;
