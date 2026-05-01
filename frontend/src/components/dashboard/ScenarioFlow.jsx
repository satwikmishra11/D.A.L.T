import React from 'react';
import { UserCircle, Key, Database, CreditCard, ArrowRight, Activity } from 'lucide-react';

const steps = [
  { id: 1, name: 'User Authentication', icon: Key, status: 'active', latency: '45ms', load: '1200 RPS' },
  { id: 2, name: 'Fetch Profile Data', icon: UserCircle, status: 'warning', latency: '210ms', load: '1150 RPS' },
  { id: 3, name: 'Process Transaction', icon: CreditCard, status: 'pending', latency: '-', load: 'Waiting...' },
  { id: 4, name: 'Update Database', icon: Database, status: 'pending', latency: '-', load: 'Waiting...' },
];

const ScenarioFlow = () => {
  return (
    <div className="card p-6 h-full flex flex-col">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h3 className="font-bold text-xl text-gray-900">Active Scenario Flow</h3>
          <p className="text-sm text-gray-500">Checkout Flow - Simulation V2</p>
        </div>
        <div className="px-3 py-1 bg-blue-50 text-blue-600 border border-blue-100 rounded-full text-xs font-bold flex items-center gap-2">
            <Activity size={14} className="animate-pulse" /> Running
        </div>
      </div>

      <div className="flex-1 flex flex-col justify-center max-w-2xl mx-auto w-full relative">
        {/* Connecting Line behind nodes */}
        <div className="absolute left-1/2 top-8 bottom-8 w-1 bg-gray-100 -translate-x-1/2 z-0 hidden md:block"></div>

        <div className="space-y-6 relative z-10">
          {steps.map((step, index) => {
            const Icon = step.icon;
            const isActive = step.status === 'active';
            const isWarning = step.status === 'warning';
            
            let statusClasses = "bg-white border-gray-200 text-gray-400";
            if (isActive) statusClasses = "bg-white border-blue-500 text-blue-600 shadow-[0_0_15px_rgba(59,130,246,0.3)] animate-pulse-slow";
            if (isWarning) statusClasses = "bg-white border-yellow-500 text-yellow-600 shadow-[0_0_15px_rgba(234,179,8,0.3)]";

            return (
              <div key={step.id} className="relative group cursor-pointer">
                {/* Visual Connection for mobile (vertical line) */}
                {index !== steps.length - 1 && (
                    <div className="absolute left-8 top-16 w-0.5 h-6 bg-gray-200 md:hidden z-0"></div>
                )}
                
                <div className={`flex flex-col md:flex-row items-center gap-4 p-4 rounded-xl border-2 transition-all duration-300 hover:shadow-md bg-white z-10 relative
                    ${isActive ? 'border-blue-200 bg-blue-50/30' : isWarning ? 'border-yellow-200 bg-yellow-50/30' : 'border-gray-100'}
                `}>
                  <div className={`w-12 h-12 shrink-0 rounded-full flex items-center justify-center border-2 transition-colors ${statusClasses}`}>
                    <Icon size={20} strokeWidth={2.5} />
                  </div>
                  
                  <div className="flex-1 text-center md:text-left">
                    <h4 className="font-bold text-gray-900">{step.name}</h4>
                    <p className="text-xs text-gray-500 mt-0.5">Step {step.id} of {steps.length}</p>
                  </div>

                  {/* Metrics Badge */}
                  <div className="flex gap-4 items-center">
                    <div className="text-right">
                        <div className={`text-xs font-bold ${isWarning ? 'text-yellow-600' : 'text-gray-900'}`}>{step.latency}</div>
                        <div className="text-[10px] text-gray-400 uppercase tracking-wider">Latency</div>
                    </div>
                    <div className="h-8 w-px bg-gray-200 hidden md:block"></div>
                    <div className="text-right hidden md:block">
                        <div className="text-xs font-bold text-gray-900">{step.load}</div>
                        <div className="text-[10px] text-gray-400 uppercase tracking-wider">Load</div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default ScenarioFlow;
