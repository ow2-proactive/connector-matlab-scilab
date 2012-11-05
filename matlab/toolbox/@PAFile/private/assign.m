function this=assign(this, varargin)
if (mod(length(varargin),2) ~= 0) && (length(varargin) ~= 1)
    error(['Invalid number of arguments : ' num2str(length(varargin))]);
end
if ~isscalar(this)
    error(['Invalid size of this : ' num2str(size(this))]);
end
if strcmp(class(varargin{1}), 'PAFile')
    input = varargin{1};
else
    for i=1:2:length(varargin)
        attrib = varargin{i};
        value = varargin{i+1};
        checkValidity(attrib,value);
        input.(attrib) = value;
    end
end
fn = fieldnames(input);
for i=1:length(fn)
    attrib = fn{i};
    value = input.(fn{i});    
    switch attrib

        case 'Path'
            this.Path = value;
        case 'Space'
            this.Space = value;
        otherwise
            error(['unknown attribute :' attrib]);
    end
end
end